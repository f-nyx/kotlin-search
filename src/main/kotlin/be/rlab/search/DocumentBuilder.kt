package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.Hashes.generateId
import be.rlab.search.model.Document
import be.rlab.search.model.Field
import be.rlab.search.model.FieldType
import java.util.*

/** Builder to create [Document]s.
 */
class DocumentBuilder private constructor (
    private val namespace: String,
    private val language: Language,
    private val version: String
){
    companion object {
        fun new(
            namespace: String,
            language: Language,
            version: String,
            callback: DocumentBuilder.() -> Unit
        ): DocumentBuilder {
            val builder = DocumentBuilder(namespace, language, version)
            callback(builder)
            return builder
        }
    }

    class FieldModifiers(
        internal var stored: Boolean,
        internal var indexed: Boolean
    ) {
        fun store(stored: Boolean = true) {
            this.stored = stored
        }

        fun index(indexed: Boolean = true) {
            this.indexed = indexed
        }
    }

    private val fields: MutableList<Field> = mutableListOf()
    private var id: UUID = UUID.randomUUID()

    /** Sets the document identifier.
     * @param docId Unique document identifier.
     */
    fun id(docId: UUID): DocumentBuilder {
        id = docId
        return this
    }

    /** Creates a new text field.
     *
     * By default text fields are indexed and stored.
     *
     * @param name Field name.
     * @param value Field value.
     * @param callback Callback to set field modifiers.
     */
    fun text(
        name: String,
        value: String,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder {
        fields += withModifiers(Field(name, value, FieldType.TEXT), callback)
        return this
    }

    /** Creates a new string field.
     *
     * String fields are saved as single terms and they're not indexed.
     * By default String fields are stored.
     *
     * @param name Field name.
     * @param value Field value.
     * @param callback Callback to set field modifiers.
     */
    fun string(
        name: String,
        value: String,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder {
        fields += withModifiers(Field(name, value, FieldType.STRING), callback)
        return this
    }

    /** Creates a new int field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     * @param callback Callback to set field modifiers.
     */
    fun int(
        name: String,
        vararg value: Int,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder {
        fields += withModifiers(Field(name, value, FieldType.INT), callback)
        return this
    }

    /** Creates a new long field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     * @param callback Callback to set field modifiers.
     */
    fun long(
        name: String,
        vararg value: Long,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder {
        fields += withModifiers(Field(name, value, FieldType.LONG), callback)
        return this
    }

    /** Creates a new float field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     * @param callback Callback to set field modifiers.
     */
    fun float(
        name: String,
        vararg value: Float,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder {
        fields += withModifiers(Field(name, value, FieldType.FLOAT), callback)
        return this
    }

    /** Creates a new double field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     * @param value Field value.
     * @param callback Callback to set field modifiers.
     */
    fun double(
        name: String,
        vararg value: Double,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder {
        fields += withModifiers(Field(name, value, FieldType.DOUBLE), callback)
        return this
    }

    /** Builds the document.
     */
    fun build(): Document {
        return Document.new(
            id = generateId(id, language),
            namespace = namespace,
            fields = fields.toList(),
            version = version
        )
    }

    private fun withModifiers(
        field: Field,
        callback: FieldModifiers.() -> Unit
    ): Field {
        val modifiers = FieldModifiers(
            stored = field.stored,
            indexed = field.indexed
        )

        callback(modifiers)

        return field.copy(
            stored = modifiers.stored,
            indexed = modifiers.indexed
        )
    }
}
