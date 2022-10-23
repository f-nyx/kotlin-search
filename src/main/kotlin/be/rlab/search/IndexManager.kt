package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.Hashes.getLanguage
import be.rlab.search.model.*
import be.rlab.search.model.Field
import be.rlab.search.model.FieldType
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.*
import org.apache.lucene.document.Field.Store
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
import org.apache.lucene.document.Document as LuceneDocument

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
        const val CURRENT_VERSION: String = "2"

        const val DEFAULT_LIMIT: Int = 1000
        internal const val PRIVATE_FIELD_PREFIX: String = "private::"
        internal const val ID_FIELD: String = "id"
        internal const val NAMESPACE_FIELD: String = "namespace"
        internal const val VERSION_FIELD: String = "version"
        private const val FIELD_TYPE: String = "type"
        private val NUMERIC_TYPES: List<FieldType> = listOf(
            FieldType.INT, FieldType.LONG, FieldType.FLOAT, FieldType.DOUBLE
        )
    }

    /** Indexes per language. */
    private val indexes: MutableMap<Language, Index> = Language.values().associateWith { language ->
        val indexDir: Directory = FSDirectory.open(File(indexPath, language.name.lowercase()).toPath())
        val analyzer: Analyzer = AnalyzerFactory.newAnalyzer(language)
        val indexWriter = IndexWriter(indexDir, IndexWriterConfig(analyzer)).apply {
            commit()
        }
        val indexReader: IndexReader = DirectoryReader.open(indexDir)

        Index(
            language = language,
            analyzer = analyzer,
            indexReader = indexReader,
            indexWriter = indexWriter
        )
    }.toMutableMap()

    /** Returns the index for the specified language.
     * @param language Language of the required index.
     * @return The required index.
     */
    fun index(language: Language): Index {
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
        val indexWriter: IndexWriter = indexes.getValue(language).indexWriter

        indexWriter.addDocument(LuceneDocument().apply {
            add(StringField(privateField(ID_FIELD, document.version), document.id, Store.YES))
            add(StringField(privateField(NAMESPACE_FIELD, document.version), document.namespace, Store.YES))

            document.fields.forEach { field ->
                val newField = when(field.type) {
                    FieldType.STRING ->
                        StringField(field.name, field.value as String, if (field.stored) {
                            Store.YES
                        } else {
                            Store.NO
                        })
                    FieldType.TEXT -> TextField(field.name, field.value as String, if (field.stored) {
                        Store.YES
                    } else {
                        Store.NO
                    })
                    FieldType.INT -> IntPoint(field.name, *toArray(field.value) as IntArray)
                    FieldType.LONG -> LongPoint(field.name, *toArray(field.value) as LongArray)
                    FieldType.FLOAT -> FloatPoint(field.name, *toArray(field.value) as FloatArray)
                    FieldType.DOUBLE -> DoublePoint(field.name, *toArray(field.value) as DoubleArray)
                }

                if (field.stored && NUMERIC_TYPES.contains(field.type)) {
                    newField.numericValue()?.let { value ->
                        add(when (newField) {
                            is IntPoint -> StoredField(newField.name(), value.toInt())
                            is LongPoint -> StoredField(newField.name(), value.toLong())
                            is FloatPoint -> StoredField(newField.name(), value.toFloat())
                            is DoublePoint -> StoredField(newField.name(), value.toDouble())
                            else -> throw RuntimeException("unknown numeric field type: ${field.type}")
                        })
                    }
                }

                add(newField)
                add(StringField(privateField("${field.name}!!$FIELD_TYPE", document.version), field.type.name, Store.YES))
                add(StringField(privateField(VERSION_FIELD), document.version, Store.YES))
            }
        })
    }

    /** Retrieves a document by id.
     * @param documentId Id of the required document.
     * @return the required document, or null if it does not exist.
     */
    fun get(documentId: String): Document? {
        return with(searcher(getLanguage(documentId))) {
            val topDocs = search(TermQuery(Term(privateField(ID_FIELD), documentId)), 1)
            transform(this, topDocs).docs.firstOrNull()
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
        val query = QueryBuilder.query(namespace, language).apply {
            builder(this)
        }

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
        val query = QueryBuilder.query(namespace, language).apply(builder)
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
                transform(this, search(query.build(), limit))
            } else {
                val scoreDoc = ScoreDoc(
                    cursor.docId,
                    cursor.score,
                    cursor.shardIndex
                )
                transform(this, searchAfter(scoreDoc, query.build(), limit))
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

    private fun transform(
        searcher: IndexSearcher,
        topDocs: TopDocs
    ): SearchResult {

        val resolvedDocs: List<Document> = topDocs.scoreDocs.map { scoreDoc ->
            val luceneDoc = searcher.doc(scoreDoc.doc)
            val version: String = luceneDoc.getField(privateField(VERSION_FIELD)).stringValue() ?: "1"
            val id: String = luceneDoc.getField(privateField(ID_FIELD, version)).stringValue()

            Document.new(
                id = id,
                namespace = luceneDoc.getField(privateField(NAMESPACE_FIELD, version)).stringValue(),
                version = version,
                fields = luceneDoc.fields.filter { field ->
                    when(version) {
                        "1" ->
                            field.name() != ID_FIELD &&
                            field.name() != NAMESPACE_FIELD &&
                            field.name() != privateField(VERSION_FIELD) &&
                            !field.name().contains("!!")
                        "2" -> !field.name().startsWith(PRIVATE_FIELD_PREFIX)
                        else -> throw RuntimeException("invalid document version: $version")
                    }
                }.map { field: IndexableField ->
                    Field(
                        name = field.name(),
                        value = field.numericValue()
                            ?: field.stringValue()
                            ?: field.binaryValue()
                            ?: field.readerValue(),
                        type = FieldType.valueOf(
                            luceneDoc.getField(privateField("${field.name()}!!$FIELD_TYPE", version)).stringValue()
                        )
                    )
                }
            )
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

    private fun toArray(source: Any): Any {
        return when (source) {
            is IntArray, is LongArray, is FloatArray, is DoubleArray -> source
            is Int -> intArrayOf(source)
            is Long -> longArrayOf(source)
            is Float -> floatArrayOf(source)
            is Double -> doubleArrayOf(source)
            else -> throw RuntimeException("Unsupported numeric value: $source")
        }
    }

    private fun privateField(
        name: String,
        version: String = CURRENT_VERSION
    ): String {
        return when(version) {
            "1" -> name
            "2" -> "$PRIVATE_FIELD_PREFIX$name"
            else -> throw RuntimeException("invalid document version: $version")
        }
    }
}
