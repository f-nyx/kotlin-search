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
     * By default text fields are indexed and stored.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun text(
        name: String,
        value: String
    ): Field {
        fields += Field(name, value, FieldType.TEXT)
        return fields.last()
    }

    /** Creates a new string field.
     *
     * String fields are saved as single terms and they're not indexed.
     * By default String fields are stored.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun string(
        name: String,
        value: String
    ): Field {
        fields += Field(name, value, FieldType.STRING)
        return fields.last()
    }

    /** Creates a new int field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun int(
        name: String,
        vararg value: Int
    ): Field {
        fields += Field(name, value, FieldType.INT)
        return fields.last()
    }

    /** Creates a new long field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun long(
        name: String,
        vararg value: Long
    ): Field {
        fields += Field(name, value, FieldType.LONG)
        return fields.last()
    }

    /** Creates a new float field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun float(
        name: String,
        vararg value: Float
    ): Field {
        fields += Field(name, value, FieldType.FLOAT)
        return fields.last()
    }

    /** Creates a new double field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     */
    fun double(
        name: String,
        vararg value: Double
    ): Field {
        fields += Field(name, value, FieldType.DOUBLE)
        return fields.last()
    }

    /** Stores the specified field in the index.
     * @param field Field to store.
     */
    fun store(field: Field): Field {
        val storedField = field.store()

        fields.replaceAll { existingField ->
            if (storedField.name == existingField.name) {
                storedField
            } else {
                existingField
            }
        }

        return storedField
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
