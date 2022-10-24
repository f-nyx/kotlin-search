package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.Hashes.getLanguage
import be.rlab.search.LuceneIndex.Companion.ID_FIELD
import be.rlab.search.LuceneIndex.Companion.NAMESPACE_FIELD
import be.rlab.search.LuceneIndex.Companion.privateField
import be.rlab.search.model.*
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.*
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/** Multi-language full-text search index.
 *
 * It creates an index for each language. It means that [Document]s only exists in the index for its language.
 * Multi-language documents must be saved for each language.
 *
 * The index-per-language pattern allows to properly report statistics and search scores. The counterpart is that
 * searching for documents in multiple languages can be expensive, but it is very fast for use cases where only
 * single-language operations are required.
 *
 * Searching for documents in multiple languages is not yet supported.
 *
 * This implementation uses a file system based index, so changes requires synchronization before the index is
 * closed. If you don't call [sync] before shutting down the application, all pending changes will be lost.
 *
 * NOTE: [IndexManager] instances are thread safe.
 */
class IndexManager(
    /** Path to store indexes. */
    indexPath: String
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IndexManager::class.java)
        const val DEFAULT_LIMIT: Int = 1000
    }

    /** Indexes per language. */
    private val indexes: MutableMap<Language, LuceneIndex> = Language.values().associateWith { language ->
        val indexDir: Directory = FSDirectory.open(File(indexPath, language.name.lowercase()).toPath())
        val analyzer: Analyzer = AnalyzerFactory.newAnalyzer(language)
        val indexWriter = IndexWriter(indexDir, IndexWriterConfig(analyzer)).apply {
            commit()
        }
        val indexReader: IndexReader = DirectoryReader.open(indexDir)

        LuceneIndex(
            language = language,
            analyzer = analyzer,
            indexReader = indexReader,
            indexWriter = indexWriter
        )
    }.toMutableMap()

    /** Registered document schemas. */
    private val schemas: MutableMap<String, DocumentSchema<*>> = mutableMapOf()

    /** Returns the index for the specified language.
     * @param language Language of the required index.
     * @return The required index.
     */
    fun index(language: Language): LuceneIndex {
        indexReader(language)
        return indexes.getValue(language)
    }

    /** Analyzes and indexes a document.
     *
     * @param namespace Document namespace.
     * @param language Document language.
     * @param builder Callback to build the document using a [DocumentBuilder].
     */
    fun index(
        namespace: String,
        language: Language,
        builder: DocumentBuilder.() -> Unit
    ) {
        index(DocumentBuilder.new(namespace, language, "1", builder).build())
    }

    /** Analyzes and indexes a document.
     * @param document Document to index.
     */
    fun index(document: Document) {
        val language: Language = getLanguage(document.id)
        val index: LuceneIndex = indexes.getValue(language)
        index.addDocument(document)
    }

    /** Retrieves a document by id.
     * @param documentId Id of the required document.
     * @return the required document, or null if it does not exist.
     */
    fun get(documentId: String): Document? {
        val language = getLanguage(documentId)
        return with(searcher(language)) {
            val topDocs = search(TermQuery(Term(privateField(ID_FIELD), documentId)), 1)
            transform(index(language), this, topDocs).docs.firstOrNull()
        }
    }

    /** Counts search results.
     *
     * @param namespace Documents namespace.
     * @param language Language of the index to search.
     * @param builder Query builder.
     */
    fun count(
        namespace: String,
        language: Language,
        builder: QueryBuilder.() -> Unit
    ): Int {
        val schema = schemas[namespace]
        val query = schema?.let {
            QueryBuilder.forSchema(schema, language, NAMESPACE_FIELD).apply(builder)
        } ?: QueryBuilder.query(namespace, language).apply(builder)

        return with(searcher(query.language)) {
            count(query.build())
        }
    }

    /** Search for documents in a specific language.
     *
     * The query builder provides a flexible interface to build Lucene queries.
     *
     * The cursor and the limit allow to paginate the search results. If you provide a cursor returned
     * in a previous [SearchResult], this method resumes the search from there.
     *
     * @param namespace Documents namespace.
     * @param language Language of the index to search.
     * @param cursor Cursor to resume a paginated search.
     * @param limit Max number of results to retrieve.
     * @param builder Query builder.
     */
    fun search(
        namespace: String,
        language: Language,
        cursor: Cursor = Cursor.first(),
        limit: Int = DEFAULT_LIMIT,
        builder: QueryBuilder.() -> Unit
    ): SearchResult {
        val schema = schemas[namespace]
        val query = schema?.let {
            QueryBuilder.forSchema(schema, language, NAMESPACE_FIELD).apply(builder)
        } ?: QueryBuilder.query(namespace, language).apply(builder)
        return search(query, cursor, limit)
    }

    /** Search for documents.
     *
     * The cursor and the limit allow to paginate the search results. If you provide a cursor returned
     * in a previous [SearchResult], this method resumes the search from there.
     *
     * @param query Query builder ready for search.
     * @param cursor Cursor to resume a paginated search.
     * @param limit Max number of results to retrieve.
     */
    fun search(
        query: QueryBuilder,
        cursor: Cursor = Cursor.first(),
        limit: Int = DEFAULT_LIMIT
    ): SearchResult {
        return with(searcher(query.language)) {
            if (cursor.isFirst()) {
                transform(index(query.language), this, search(query.build(), limit))
            } else {
                val scoreDoc = ScoreDoc(
                    cursor.docId,
                    cursor.score,
                    cursor.shardIndex
                )
                transform(index(query.language), this, searchAfter(scoreDoc, query.build(), limit))
            }
        }
    }

    /** Search for documents in a specific language.
     *
     * It produces a sequence that goes through all search results. The [limit] is the
     * size of each [SearchResult]. It will query the index until it returns no more results.
     *
     * @param namespace Documents namespace.
     * @param language Language of the index to search.
     * @param limit Max number of results to retrieve.
     * @param builder Query builder.
     */
    fun find(
        namespace: String,
        language: Language,
        limit: Int = DEFAULT_LIMIT,
        builder: QueryBuilder.() -> Unit
    ): Sequence<Document> {
        var result: SearchResult = search(namespace, language, Cursor.first(), limit, builder)
        var docs: Iterator<Document> = result.docs.iterator()

        fun next(): Document? = when {
            docs.hasNext() -> docs.next()
            !docs.hasNext() && result.next != null -> {
                result = search(namespace, language, result.next!!, limit, builder)
                docs = result.docs.iterator()
                next()
            }
            else -> null
        }

        return generateSequence {
            next()
        }
    }

    /** Synchronizes and writes all pending changes to disk.
     */
    fun sync() {
        logger.debug("writing all indexes to disk")
        Language.values().forEach { language ->
            indexes.getValue(language).indexWriter.commit()
        }
    }

    /** Closes the index and optionally synchronizes all pending changes.
     * @param sync true to synchronize changes.
     */
    fun close(sync: Boolean = true) {
        logger.debug("closing index manager")
        if (sync) {
            sync()
        }

        Language.values().forEach { language ->
            indexes.getValue(language).indexWriter.close()
        }
    }

    fun addSchema(
        namespace: String,
        builder: SchemaBuilder.() -> Unit
    ): IndexManager = apply {
        schemas[namespace] = SchemaBuilder.new(namespace, builder).build()
    }

    private fun transform(
        index: LuceneIndex,
        searcher: IndexSearcher,
        topDocs: TopDocs
    ): SearchResult {

        val resolvedDocs: List<Document> = topDocs.scoreDocs.map { scoreDoc ->
            val luceneDoc = searcher.doc(scoreDoc.doc)
            index.map(luceneDoc)
        }

        return SearchResult.new(
            results = resolvedDocs,
            total = topDocs.totalHits.value,
            next = if (resolvedDocs.isNotEmpty()) {
                val nextDoc = topDocs.scoreDocs.last()
                Cursor(
                    nextDoc.doc,
                    nextDoc.score,
                    nextDoc.shardIndex
                )
            } else {
                null
            }
        )
    }

    private fun searcher(language: Language): IndexSearcher {
        logger.debug("creating index searcher for language: $language")
        return IndexSearcher(indexReader(language))
    }

    private fun indexReader(language: Language): IndexReader {
        logger.debug("loading index reader for language: $language")
        val indexReader = indexes.getValue(language).indexReader

        DirectoryReader.openIfChanged(indexReader as DirectoryReader)?.let { nextReader ->
            logger.debug("index changed, reloaded from disk")
            indexes[language] = indexes.getValue(language).update(nextReader)
        }

        return indexes.getValue(language).indexReader
    }
}
