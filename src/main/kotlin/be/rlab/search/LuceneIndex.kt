package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.LuceneFieldUtils.PRIVATE_FIELD_PREFIX
import be.rlab.search.LuceneFieldUtils.addField
import be.rlab.search.LuceneFieldUtils.privateField
import be.rlab.search.model.*
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.StringField
import org.apache.lucene.index.*
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.lucene.document.Document as LuceneDocument
import org.apache.lucene.document.Field as LuceneField

/** Represents a Lucene index.
 *
 * @param language Index language.
 * @param analyzer Analyzer used to pre-process text.
 * @param indexReader Reader to query the index.
 * @param indexWriter Writer to index and to store data into the index.
 */
class LuceneIndex(
    val language: Language,
    val analyzer: Analyzer,
    var indexReader: IndexReader,
    val indexWriter: IndexWriter
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LuceneIndex::class.java)

        const val CURRENT_VERSION: String = "2"
        internal const val ID_FIELD: String = "id"
        internal const val NAMESPACE_FIELD: String = "namespace"
        internal const val VERSION_FIELD: String = "version"
        /** @deprecated now it's stored in the metadata field. */
        internal const val TYPE_FIELD: String = "type"
        internal const val METADATA_FIELD: String = "meta"
    }

    /** Adds a document to the index.
     *
     * Take into account that writing to disk is an async operation, if you need
     * to access the document immediately after indexing you need to call [IndexManager.sync].
     *
     * @param document Document to add.
     * @return Lucene sequence number for this operation.
     */
    fun addDocument(document: Document): Long {
        return indexWriter.addDocument(map(document))
    }

    /** Retrieves all documents for the specified search results.
     * @param hits Search results.
     * @return the documents from the search results.
     */
    fun getDocuments(hits: TopDocs): List<Document> {
        val storedFields = indexReader.storedFields()
        return hits.scoreDocs.map { hit ->
            storedFields.document(hit.doc)
        }.map { document -> map(document) }
    }

    /** Retrieves a document by id.
     * @param documentId Id of the required document.
     * @return the required document, or null if it does not exist.
     */
    fun getDocumentById(documentId: String): Document? {
        val language = Hashes.getLanguage(documentId)
        return with(searcher(language)) {
            val topDocs = search(TermQuery(Term(be.rlab.search.LuceneFieldUtils.privateField(be.rlab.search.LuceneIndex.ID_FIELD), documentId)), 1)
            transform(topDocs).docs.firstOrNull()
        }
    }

    fun count(queryBuilder: QueryBuilder): Int {
        return with(searcher(queryBuilder.language)) {
            count(queryBuilder.build())
        }
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
        limit: Int = IndexManager.DEFAULT_LIMIT
    ): SearchResult {
        return with(searcher(queryBuilder.language)) {
            val sort = queryBuilder.sort()
            if (cursor.isFirst()) {
                if (sort != null) {
                    transform(search(queryBuilder.build(), limit, queryBuilder.sort()))
                } else {
                    transform(search(queryBuilder.build(), limit))
                }
            } else {
                val scoreDoc = ScoreDoc(
                    cursor.docId,
                    cursor.score,
                    cursor.shardIndex
                )
                if (sort != null) {
                    transform(searchAfter(scoreDoc, queryBuilder.build(), limit, queryBuilder.sort()))
                } else {
                    transform(searchAfter(scoreDoc, queryBuilder.build(), limit))
                }
            }
        }
    }

    /** Search for documents in a specific language.
     *
     * It produces a sequence that goes through all search results. The [limit] is the
     * size of each [SearchResult]. It will query the index until it returns no more results.
     *
     * @param queryBuilder Query builder ready for search.
     * @param limit Max number of results to retrieve.
     */
    fun find(
        queryBuilder: QueryBuilder,
        limit: Int = IndexManager.DEFAULT_LIMIT
    ): Sequence<Document> {
        var result: SearchResult = search(queryBuilder, Cursor.first(), limit)
        var docs: Iterator<Document> = result.docs.iterator()

        fun next(): Document? = when {
            docs.hasNext() -> docs.next()
            !docs.hasNext() && result.next != null -> {
                result = search(queryBuilder, result.next!!, limit)
                docs = result.docs.iterator()
                next()
            }
            else -> null
        }

        return generateSequence {
            next()
        }
    }

    /** Maps the internal Document model to a Lucene document.
     * @param document Document to map.
     * @return The Lucene Document object.
     */
    @Suppress("UNCHECKED_CAST")
    fun map(document: Document): LuceneDocument = LuceneDocument().apply {
        add(StringField(privateField(ID_FIELD, document.version), document.id, LuceneField.Store.YES))
        add(StringField(privateField(NAMESPACE_FIELD, document.version), document.namespace, LuceneField.Store.YES))
        add(StringField(privateField(VERSION_FIELD, document.version), document.version, LuceneField.Store.YES))
        document.fields.forEach { field: Field<*> -> addField(field as Field<Any>, document.version) }
    }

    /** Maps a Lucene document to the Document model.
     * @param luceneDoc Lucene document to map.
     * @return The Document object.
     */
    fun map(luceneDoc: LuceneDocument): Document {
        val version: String = luceneDoc.getField(privateField(VERSION_FIELD)).stringValue() ?: "1"
        val id: String = luceneDoc.getField(privateField(ID_FIELD, version)).stringValue()

        return Document.new(
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
            }.fold(mutableMapOf<String, Field<Any>>()) { fieldsMap, field: IndexableField ->
                val value = field.numericValue()
                    ?: field.stringValue()
                    ?: field.binaryValue()
                    ?: field.readerValue().readText()
                if (!fieldsMap.containsKey(field.name())) {
                    val metadata = luceneDoc.getField(privateField("${field.name()}!!$METADATA_FIELD", version))?.let {
                        FieldMetadata.deserialize(it.stringValue())
                    }
                    val fieldType = metadata?.type ?: FieldType.valueOf(
                        luceneDoc.getField(privateField("${field.name()}!!$TYPE_FIELD", version)).stringValue()
                    )

                    fieldsMap[field.name()] = Field(
                        name = field.name(),
                        values = listOf(value),
                        type = fieldType,
                        stored = metadata?.stored ?: fieldType.stored,
                        indexed = metadata?.indexed ?: fieldType.indexed,
                        docValues = metadata?.docValues ?: false
                    )
                } else {
                    fieldsMap[field.name()] = fieldsMap.getValue(field.name()).addValues(listOf(value))
                }
                fieldsMap
            }.values.toList()
        )
    }

    /** Synchronizes and writes all pending changes to disk.
     */
    fun sync() {
        logger.debug("writing changes to disk")
        indexWriter.commit()
    }

    /** Closes the index and optionally synchronizes all pending changes.
     * @param sync true to synchronize changes.
     */
    fun close(sync: Boolean = true) {
        logger.debug("closing lucene index")
        if (sync) {
            sync()
        }

        indexWriter.close()
    }

    /** Reopens the index if it detects changes on disk.
     */
    fun reopenIfChanged(): LuceneIndex = apply {
        logger.debug("checking if reopen is required")

        DirectoryReader.openIfChanged(indexReader as DirectoryReader)?.let { nextReader ->
            logger.debug("index changed, reloaded from disk")
            indexReader = nextReader
        }
    }

    private fun searcher(language: Language): IndexSearcher {
        logger.debug("creating index searcher for language: $language")
        return IndexSearcher(indexReader)
    }

    private fun transform(topDocs: TopDocs): SearchResult {
        val resolvedDocs: List<Document> = getDocuments(topDocs)

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
}
