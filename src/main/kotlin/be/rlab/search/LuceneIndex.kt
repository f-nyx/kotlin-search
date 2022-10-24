package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.model.Document
import be.rlab.search.model.FieldType
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexableField
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
        internal const val PRIVATE_FIELD_PREFIX: String = "private!!"
        internal const val ID_FIELD: String = "id"
        internal const val NAMESPACE_FIELD: String = "namespace"
        internal const val VERSION_FIELD: String = "version"
        private const val FIELD_TYPE: String = "type"
        private val NUMERIC_TYPES: List<FieldType> = listOf(
            FieldType.INT, FieldType.LONG, FieldType.FLOAT, FieldType.DOUBLE
        )

        internal fun privateField(
            name: String,
            version: String = CURRENT_VERSION
        ): String {
            return when (version) {
                "1" -> name
                "2" -> "${PRIVATE_FIELD_PREFIX}$name"
                else -> throw RuntimeException("invalid document version: $version")
            }
        }
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
    fun map(document: Document): LuceneDocument = LuceneDocument().apply {
        add(StringField(privateField(ID_FIELD, document.version), document.id, Field.Store.YES))
        add(StringField(privateField(NAMESPACE_FIELD, document.version), document.namespace, Field.Store.YES))

        document.fields.forEach { field ->
            val newField = when (field.type) {
                FieldType.STRING ->
                    StringField(field.name, field.value as String, if (field.stored) {
                        Field.Store.YES
                    } else {
                        Field.Store.NO
                    })

                FieldType.TEXT -> TextField(field.name, field.value as String, if (field.stored) {
                    Field.Store.YES
                } else {
                    Field.Store.NO
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
            add(StringField(privateField("${field.name}!!${FIELD_TYPE}", document.version), field.type.name, Field.Store.YES))
            add(StringField(privateField(VERSION_FIELD), document.version, Field.Store.YES))
        }
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
            }.map { field: IndexableField ->
                be.rlab.search.model.Field(
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
}
