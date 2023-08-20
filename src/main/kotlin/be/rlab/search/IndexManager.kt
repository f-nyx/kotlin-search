package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.Hashes.getLanguage
import be.rlab.search.model.*
import be.rlab.search.schema.DocumentSchemaBuilder
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.Similarity
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
    indexPath: String,
    /** Index configuration. */
    indexConfig: IndexConfig = IndexConfig.default()
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IndexManager::class.java)
        const val DEFAULT_LIMIT: Int = 1000
    }

    /** Builder to construct a new configured [IndexManager].
     */
    class Builder(private val indexPath: String) {
        private var similarity: Similarity = BM25Similarity()
        private var supportedLanguages: List<Language> = Language.entries

        fun withSimilarity(newSimilarity: Similarity): Builder = apply {
            similarity = newSimilarity
        }

        fun forLanguages(languages: List<Language>): Builder = apply {
            supportedLanguages = languages
        }

        fun build(): IndexManager {
            return IndexManager(indexPath, IndexConfig.new(
                supportedLanguages = supportedLanguages,
                similarity = similarity
            ))
        }
    }

    /** Indexes per language. */
    private val indexes: MutableMap<Language, LuceneIndex> = indexConfig.supportedLanguages.associateWith { language ->
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
            indexWriter = indexWriter,
            indexConfig = indexConfig
        )
    }.toMutableMap()

    /** Registered document schemas. */
    private val schemas: MutableMap<String, DocumentSchema> = mutableMapOf()

    /** Returns the index for the specified language.
     * @param language Language of the required index.
     * @return The required index.
     */
    fun openIndex(language: Language): LuceneIndex {
        logger.debug("opening index for language: $language")
        return indexes.getValue(language).reopenIfChanged()
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
        index(
            DocumentBuilder.new(
                namespace,
                language,
                LuceneIndex.CURRENT_VERSION,
                schemas[namespace]
            ).apply(builder).build()
        )
    }

    /** Analyzes and indexes a document.
     * @param document Document to index.
     */
    fun index(document: Document) {
        openIndex(getLanguage(document.id)).addDocument(document)
    }

    /** Retrieves a document by id.
     * @param documentId Id of the required document.
     * @return the required document, or null if it does not exist.
     */
    fun get(documentId: String): Document? {
        return openIndex(getLanguage(documentId)).getDocumentById(documentId)
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
        val queryBuilder = schema?.let {
            QueryBuilder.forSchema(schema, language).apply(builder)
        } ?: QueryBuilder.query(namespace, language).apply(builder)

        return openIndex(language).count(queryBuilder)
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
        val queryBuilder = schema?.let {
            QueryBuilder.forSchema(schema, language).apply(builder)
        } ?: QueryBuilder.query(namespace, language).apply(builder)
        return openIndex(language).search(queryBuilder, cursor, limit)
    }

    /** Search for documents.
     *
     * The cursor and the limit allow to paginate the search results. If you provide a cursor returned
     * in a previous [SearchResult], this method resumes the search from there.
     *
     * @param queryBuilder Query builder ready for search.
     * @param cursor Cursor to resume a paginated search.
     * @param limit Max number of results to retrieve.
     */
    fun search(
        queryBuilder: QueryBuilder,
        cursor: Cursor = Cursor.first(),
        limit: Int = DEFAULT_LIMIT
    ): SearchResult {
        return openIndex(queryBuilder.language).search(queryBuilder, cursor, limit)
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
        val schema = schemas[namespace]
        val queryBuilder = schema?.let {
            QueryBuilder.forSchema(schema, language).apply(builder)
        } ?: QueryBuilder.query(namespace, language).apply(builder)

        return openIndex(language).find(queryBuilder, limit)
    }

    /** Synchronizes and writes all pending changes to disk.
     */
    fun sync() {
        logger.debug("writing all indexes to disk")
        Language.entries.forEach { language ->
            openIndex(language).sync()
        }
    }

    /** Closes the index and optionally synchronizes all pending changes.
     * @param sync true to synchronize changes.
     */
    fun close(sync: Boolean = true) {
        logger.debug("closing index manager")
        Language.entries.forEach { language ->
            openIndex(language).close(sync)
        }
    }

    fun addSchema(
        namespace: String,
        builder: DocumentSchemaBuilder.() -> Unit
    ): IndexManager = apply {
        schemas[namespace] = DocumentSchemaBuilder.new(namespace, builder).build()
    }
}
