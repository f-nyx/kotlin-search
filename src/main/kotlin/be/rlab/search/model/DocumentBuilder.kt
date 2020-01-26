package be.rlab.search.model

import be.rlab.nlp.model.Language
import be.rlab.search.Hashes.generateId
import java.util.*

/** Builder to create [Document]s.
 */
class DocumentBuilder private constructor (
    private val namespace: String,
    private val language: Language
){
    companion object {
        fun new(
            namespace: String,
            language: Language,
            callback: DocumentBuilder.() -> Unit
        ): DocumentBuilder {
            val builder = DocumentBuilder(namespace, language)
            callback(builder)
            return builder
        }
    }

    private val fields: MutableList<Field> = mutableListOf()

    /** Creates a new text field.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun text(
        name: String,
        value: String
    ) {
        fields += Field(name, value, FieldType.TEXT)
    }

    /** Creates a new string field.
     *
     * String fields are saved as single terms and they're not indexed.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun string(
        name: String,
        value: String
    ) {
        fields += Field(name, value, FieldType.STRING)
    }

    /** Creates a new int field.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun int(
        name: String,
        vararg value: Int
    ) {
        fields += Field(name, value, FieldType.INT)
    }

    /** Creates a new long field.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun long(
        name: String,
        vararg value: Long
    ) {
        fields += Field(name, value, FieldType.LONG)
    }

    /** Creates a new float field.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun float(
        name: String,
        vararg value: Float
    ) {
        fields += Field(name, value, FieldType.FLOAT)
    }

    /** Creates a new double field.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun double(
        name: String,
        vararg value: Double
    ) {
        fields += Field(name, value, FieldType.DOUBLE)
    }

    /** Builds the document.
     */
    fun build(): Document {
        return Document(
            id = generateId(UUID.randomUUID(), language),
            namespace = namespace,
            language = language,
            fields = fields.toList()
        )
    }
}
