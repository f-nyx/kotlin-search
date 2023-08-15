package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.LuceneFieldUtils.PRIVATE_FIELD_PREFIX
import be.rlab.search.LuceneFieldUtils.addField
import be.rlab.search.LuceneFieldUtils.booleanValue
import be.rlab.search.LuceneFieldUtils.privateField
import be.rlab.search.model.Document
import be.rlab.search.model.Field
import be.rlab.search.model.FieldType
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
import org.apache.lucene.document.*
import org.apache.lucene.document.Field as LuceneField
import org.apache.lucene.document.Document as LuceneDocument

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
        const val CURRENT_VERSION: String = "2"
        internal const val ID_FIELD: String = "id"
        internal const val NAMESPACE_FIELD: String = "namespace"
        internal const val VERSION_FIELD: String = "version"
        internal const val TYPE_FIELD: String = "type"
        internal const val STORED_FIELD: String = "stored"
        internal const val INDEXED_FIELD: String = "indexed"
        internal const val DOC_VALUES_FIELD: String = "docValues"
    }

    /** Updates the index reader.
     * @param indexReader New index reader.
     * @return the updated index.
     */
    fun update(indexReader: IndexReader): LuceneIndex = apply {
        this.indexReader = indexReader
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

    /** Maps the internal Document model to a Lucene document.
     * @param document Document to map.
     * @return The Lucene Document object.
     */
    @Suppress("UNCHECKED_CAST")
    fun map(document: Document): LuceneDocument = LuceneDocument().apply {
        add(StringField(privateField(ID_FIELD, document.version), document.id, LuceneField.Store.YES))
        add(StringField(privateField(NAMESPACE_FIELD, document.version), document.namespace, LuceneField.Store.YES))
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
                val fieldType = FieldType.valueOf(
                    luceneDoc.getField(privateField("${field.name()}!!$TYPE_FIELD", version)).stringValue()
                )
                val value = field.numericValue()
                    ?: field.stringValue()
                    ?: field.binaryValue()
                    ?: field.readerValue().readText()
                if (!fieldsMap.containsKey(field.name())) {
                    fieldsMap[field.name()] = Field(
                        name = field.name(),
                        values = listOf(value),
                        type = fieldType,
                        stored = luceneDoc.getField(privateField(STORED_FIELD, version)).booleanValue() ?: fieldType.stored,
                        indexed = luceneDoc.getField(privateField(INDEXED_FIELD, version)).booleanValue() ?: fieldType.indexed,
                        docValues = luceneDoc.getField(privateField(DOC_VALUES_FIELD, version)).booleanValue() ?: false
                    )
                } else {
                    fieldsMap[field.name()] = fieldsMap.getValue(field.name()).addValues(listOf(value))
                }
                fieldsMap
            }.values.toList()
        )
    }
}
