package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.Hashes.generateId
import be.rlab.search.model.*
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

/** Builder to create [Document]s.
 */
class DocumentBuilder private constructor (
    private val namespace: String,
    private val language: Language,
    private val schema: DocumentSchema?,
    private val version: String
){
    companion object {
        fun new(
            namespace: String,
            language: Language,
            version: String,
            schema: DocumentSchema?,
            callback: DocumentBuilder.() -> Unit
        ): DocumentBuilder {
            val builder = DocumentBuilder(namespace, language, schema, version)
            callback(builder)
            return builder
        }

        @Suppress("UNCHECKED_CAST")
        fun<T : Any> buildFromObject(
            schema: DocumentSchema,
            source: T,
            version: String
        ): List<Document> {
            return schema.languages.map { language ->
                val properties = source::class.declaredMemberProperties
                    .filter { property -> schema.findField(property.name) != null }
                properties.fold(DocumentBuilder(schema.namespace, language, schema, version)) { builder, property ->
                    val value = (property as KProperty1<Any, *>).get(source)
                    requireNotNull(value) { "the field value cannot be null" }

                    if (value is List<*>) {
                        builder.field(property.name, values = value.toTypedArray() as Array<Any>)
                    } else {
                        builder.field(property.name, value)
                    }
                }.build()
            }
        }
    }

    /** Field-level modifiers overrides the schema and the field type options.
     */
    class FieldModifiers(
        internal var stored: Boolean,
        internal var indexed: Boolean,
        internal var docValues: Boolean
    ) {
        /** Configures whether the value of this field must be stored in the index or not.
         * @param stored True to store, false to prevent from storing the field.
         */
        fun store(stored: Boolean = true) {
            this.stored = stored
        }

        /** Configures whether this field must be indexed or not.
         * @param indexed True to index, false to prevent from indexing the field.
         */
        fun index(indexed: Boolean = true) {
            this.indexed = indexed
        }

        /** Marks this field to be stored as DocValues.
         * DocValues are a document-level fields, and they are much faster for sorting and faceting.
         * @see https://solr.apache.org/guide/6_6/docvalues.html
         */
        fun docValues(docValue: Boolean = true) {
            this.docValues = docValue
        }
    }

    private val fields: MutableList<Field<*>> = mutableListOf()
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
     * If there is a schema defined for this document, it validates the field name.
     *
     * @param name Field name.
     * @param values Field value.
     * @param callback Callback to set field modifiers.
     */
    fun text(
        name: String,
        vararg values: String,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder = apply {
        addField(name, FieldType.TEXT, values.toList(), callback)
    }

    /** Creates a new string field.
     *
     * String fields are saved as single terms and they're not indexed.
     * By default String fields are stored.
     * If there is a schema defined for this document, it validates the field name.
     *
     * @param name Field name.
     * @param values Field value.
     * @param callback Callback to set field modifiers.
     */
    fun string(
        name: String,
        vararg values: String,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder = apply {
        addField(name, FieldType.STRING, values.toList(), callback)
    }

    /** Creates a new int field.
     * By default numeric fields are not stored.
     * If there is a schema defined for this document, it validates the field name.
     *
     * @param name Field name.
     * @param values Field value.
     * @param callback Callback to set field modifiers.
     */
    fun int(
        name: String,
        vararg values: Int,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder = apply {
        addField(name, FieldType.INT, values.toList(), callback)
    }

    /** Creates a new long field.
     * By default numeric fields are not stored.
     * If there is a schema defined for this document, it validates the field name.
     *
     * @param name Field name.
     * @param values Field value.
     * @param callback Callback to set field modifiers.
     */
    fun long(
        name: String,
        vararg values: Long,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder = apply {
        addField(name, FieldType.LONG, values.toList(), callback)
    }

    /** Creates a new float field.
     * By default numeric fields are not stored.
     * If there is a schema defined for this document, it validates the field name.
     *
     * @param name Field name.
     * @param values Field value.
     * @param callback Callback to set field modifiers.
     */
    fun float(
        name: String,
        vararg values: Float,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder = apply {
        addField(name, FieldType.FLOAT, values.toList(), callback)
    }

    /** Creates a new double field.
     * By default numeric fields are not stored.
     * If there is a schema defined for this document, it validates the field name.
     *
     * @param name Field name.
     * @param values Field value.
     * @param callback Callback to set field modifiers.
     */
    fun double(
        name: String,
        vararg values: Double,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder = apply {
        addField(name, FieldType.DOUBLE, values.toList(), callback)
    }

    /** Creates a new field and resolves the type from the schema, if any.
     * This method throws an error if there is no schema defined for this document.
     * Otherwise, the data type and the field name are validated using the schema.
     *
     * @param name Field name.
     * @param values Field value.
     * @param callback Callback to set field modifiers.
     */
    fun field(
        name: String,
        vararg values: Any,
        callback: FieldModifiers.() -> Unit = {}
    ): DocumentBuilder = apply {
        require(schema != null) { "The document schema is mandatory to define a field using this function." }
        val fieldSchema = requireNotNull(schema.findField(name)) {
            "The field does not exist in the schema: name=$name"
        }
        addField(name, fieldSchema.type, values.toList(), callback)
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

    @Suppress("UNCHECKED_CAST")
    private fun<T> addField(
        name: String,
        type: FieldType,
        values: List<T>,
        callback: FieldModifiers.() -> Unit
    ) {
        // validates the field
        val fieldSchema = schema?.findField(name)?.validate(values) ?: let {
            FieldSchema.validate(name, type, values)
            null
        }
        // allows to override the resolved configuration
        val modifiers = FieldModifiers(
            fieldSchema?.stored ?: type.stored,
            fieldSchema?.indexed ?: type.indexed,
            fieldSchema?.docValues ?: false
        ).apply(callback)
        // creates or updates the field.
        val existingField: Field<T>? = fields.find { field -> field.name == name } as Field<T>?
        val resolvedField = existingField
            ?.configure(modifiers.stored, modifiers.indexed, modifiers.docValues)
            ?.addValues(values)
            ?: Field.new(
                name = name,
                type = type,
                values = values,
                stored = modifiers.stored,
                indexed = modifiers.indexed,
                docValues = modifiers.docValues
            )
        fields.removeIf { field -> field.name == name }
        fields += resolvedField
    }
}
