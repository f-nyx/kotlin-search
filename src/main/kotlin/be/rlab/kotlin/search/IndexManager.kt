package be.rlab.kotlin.search

import be.rlab.kotlin.search.Hashes.getLanguage
import be.rlab.augusto.nlp.model.*
import be.rlab.kotlin.search.model.*
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.io.File
import org.apache.lucene.document.Document as LuceneDocument
import org.apache.lucene.document.Field as LuceneField
import org.apache.lucene.document.FieldType as LuceneFieldType

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
 * NOTE: [IndexManager] instances are completely thread safe.
 *
 * @param indexPath Path to store indexes.
 */
class IndexManager(indexPath: String) {
    companion object {
        const val DEFAULT_LIMIT: Int = 1000
        const val ID_FIELD: String = "id"
        const val NAMESPACE_FIELD: String = "namespace"

        private val TEXT_FIELD = LuceneFieldType(TextField.TYPE_STORED).apply {
            setStoreTermVectors(true)
            setStoreTermVectorOffsets(true)
            setStoreTermVectorPositions(true)
        }
        private val STRING_FIELD = LuceneFieldType(StringField.TYPE_STORED).apply {
            setStoreTermVectors(true)
        }
        private const val FIELD_TYPE: String = "type"
    }

    /** Indexes per language. */
    private val indexes: MutableMap<Language, Index> = mutableMapOf()

    init {
        Language.values().map { language ->
            val indexDir: Directory = FSDirectory.open(File(indexPath, language.name.toLowerCase()).toPath())
            val analyzer: Analyzer = AnalyzerFactory.newAnalyzer(language)
            val indexWriter = IndexWriter(indexDir, IndexWriterConfig(analyzer)).apply {
                commit()
            }
            val indexReader: IndexReader = DirectoryReader.open(indexDir)

            language to Index(
                language = language,
                analyzer = analyzer,
                indexReader = indexReader,
                indexWriter = indexWriter
            )
        }.toMap(indexes)
    }

    /** Returns the index for the specified language.
     * @param language Language of the required index.
     * @return The required index.
     */
    fun index(language: Language): Index {
        indexReader(language)
        return indexes.getValue(language)
    }

    /** Analyzes and indexes a document.
     * @param document Document to index.
     */
    fun index(document: Document) {
        val indexWriter: IndexWriter = indexes.getValue(document.language).indexWriter

        indexWriter.addDocument(LuceneDocument().apply {
            add(LuceneField(
                ID_FIELD, document.id,
                STRING_FIELD
            ))
            add(LuceneField(
                NAMESPACE_FIELD, document.namespace,
                STRING_FIELD
            ))

            document.fields.forEach { field ->
                val fieldType = when(field.type) {
                    FieldType.STRING -> STRING_FIELD
                    FieldType.TEXT -> TEXT_FIELD
                }
                add(LuceneField(field.name, field.value, fieldType))
                add(LuceneField("${field.name}!!$FIELD_TYPE", field.type.name,
                    STRING_FIELD
                ))
            }
        })
    }

    /** Retrieves a document by id.
     * @param documentId Id of the required document.
     * @return the required document, or null if it does not exist.
     */
    fun get(documentId: String): Document? {
        return with(searcher(getLanguage(documentId))) {
            val topDocs = search(TermQuery(Term(ID_FIELD, documentId)), 1)
            transform(this, topDocs).results.firstOrNull()
        }
    }

    /** Searches for documents in a specific language.
     *
     * The search is performed over fields. The [fields] key is the field name and the value is the search
     * criteria for that field. Search criteria supports wildcards. For wildcards reference look at
     * [Lucene documentation](https://lucene.apache.org/core/8_3_1/core/org/apache/lucene/search/WildcardQuery.html).
     *
     * The cursor and the limit allows to paginate the search results. If you provide a cursor previously returned
     * in a [PaginatedResult], this method resumes the search from there.
     *
     * @param namespace Documents namespace.
     * @param fields Fields search criteria.
     * @param cursor Cursor to resume a paginated search.
     * @param limit Max number of results to retrieve.
     */
    fun search(
        namespace: String,
        fields: Map<String, String>,
        language: Language,
        cursor: Cursor = Cursor.first(),
        limit: Int = DEFAULT_LIMIT
    ): PaginatedResult {
        val query: BooleanQuery = fields.entries.fold(
            BooleanQuery.Builder()
        ) { query, (fieldName, value) ->
            query.add(WildcardQuery(Term(fieldName, value)), BooleanClause.Occur.MUST)
        }.add(
            TermQuery(Term(NAMESPACE_FIELD, namespace)),
            BooleanClause.Occur.MUST
        ).build()

        return with(searcher(language)) {
            if (cursor.isFirst()) {
                transform(this, search(query, limit), cursor)
            } else {
                transform(this, searchAfter(scoreDoc(cursor), query, limit))
            }
        }
    }

    /** Synchronizes and writes all pending changes to disk.
     */
    fun sync() {
        Language.values().forEach { language ->
            indexes.getValue(language).indexWriter.commit()
        }
    }

    /** Closes the index and optionally synchronizes all pending changes.
     * @param sync true to synchronize changes.
     */
    fun close(sync: Boolean = true) {
        if (sync) {
            sync()
        }

        Language.values().forEach { language ->
            indexes.getValue(language).indexWriter.close()
        }
    }

    private fun transform(
        searcher: IndexSearcher,
        topDocs: TopDocs,
        cursor: Cursor? = null
    ): PaginatedResult {

        val resolvedDocs: List<Document> = topDocs.scoreDocs.map { scoreDoc ->
            val luceneDoc = searcher.doc(scoreDoc.doc)
            val id: String = luceneDoc.getField(ID_FIELD).stringValue()

            Document(
                id = id,
                namespace = luceneDoc.getField(NAMESPACE_FIELD).stringValue(),
                language = getLanguage(id),
                fields = luceneDoc.fields.filter { field ->
                    field.name() != ID_FIELD &&
                            field.name() != NAMESPACE_FIELD &&
                            !field.name().contains("!!")
                }.map { field ->
                    Field(
                        name = field.name(),
                        value = field.stringValue(),
                        type = FieldType.valueOf(luceneDoc.getField("${field.name()}!!$FIELD_TYPE").stringValue())
                    )
                }
            )
        }

        return PaginatedResult.new(
            results = resolvedDocs,
            total = topDocs.totalHits.value,
            previous = cursor,
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
        return IndexSearcher(indexReader(language))
    }

    private fun scoreDoc(cursor: Cursor): ScoreDoc {
        return ScoreDoc(
            cursor.docId,
            cursor.score,
            cursor.shardIndex
        )
    }

    private fun indexReader(language: Language): IndexReader {
        val indexReader = indexes.getValue(language).indexReader

        DirectoryReader.openIfChanged(indexReader as DirectoryReader)?.let { nextReader ->
            indexes[language] = indexes.getValue(language).update(nextReader)
        }

        return indexes.getValue(language).indexReader
    }
}
