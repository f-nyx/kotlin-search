package be.rlab.search.schema

import be.rlab.nlp.model.Language
import be.rlab.search.annotation.IndexDocument
import be.rlab.search.annotation.IndexField
import be.rlab.search.mapper.FieldTypeMapper
import be.rlab.search.model.DocumentSchema
import be.rlab.search.model.FieldSchema
import be.rlab.search.model.FieldType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/** Builder to define a [DocumentSchema].
 */
class DocumentSchemaBuilder private constructor (
    private val namespace: String
){
    companion object {
        fun new(
            namespace: String,
            callback: DocumentSchemaBuilder.() -> Unit
        ): DocumentSchemaBuilder =
            DocumentSchemaBuilder(namespace).apply(callback)

        @Suppress("UNCHECKED_CAST")
        fun buildFromClass(
            documentType: KClass<*>,
            fieldTypeMappers: List<FieldTypeMapper>
        ): DocumentSchema {
            require(documentType.hasAnnotation<IndexDocument>()) {
                "@IndexDocument annotation not found in class: ${documentType.qualifiedName}."
            }
            val docInfo: IndexDocument = documentType.findAnnotation()
                ?: throw RuntimeException("@IndexDocument annotation not found")
            val fields = documentType.members
                .filter { member -> member is KProperty1<*, *> && member.hasAnnotation<IndexField>() }
                .flatMap { member ->
                    FieldSchemaBuilder.buildFromProperty(member as KProperty1<Any, *>, fieldTypeMappers)
                }

            return DocumentSchema(
                namespace = docInfo.namespace,
                languages = docInfo.languages.toList(),
                fields = fields
            )
        }
    }

    private val fields: MutableList<FieldSchema> = mutableListOf()

    /** Defines a new text field.
     *
     * By default text fields are indexed and stored.
     *
     * @param name Field name.
     */
    fun text(name: String, callback: FieldSchemaBuilder.() -> Unit = {}): DocumentSchemaBuilder = apply {
        fields += FieldSchemaBuilder.new(name, FieldType.TEXT, callback).build()
    }

    /** Creates a new string field.
     *
     * String fields are saved as single terms and they're not indexed.
     * By default String fields are stored.
     *
     * @param name Field name.
     */
    fun string(name: String, callback: FieldSchemaBuilder.() -> Unit = {}): DocumentSchemaBuilder = apply {
        fields += FieldSchemaBuilder.new(name, FieldType.STRING, callback).build()
    }

    /** Creates a new int field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun int(name: String, callback: FieldSchemaBuilder.() -> Unit = {}): DocumentSchemaBuilder = apply {
        fields += FieldSchemaBuilder.new(name, FieldType.INT, callback).build()
    }

    /** Creates a new long field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun long(name: String, callback: FieldSchemaBuilder.() -> Unit = {}): DocumentSchemaBuilder = apply {
        fields += FieldSchemaBuilder.new(name, FieldType.LONG, callback).build()
    }

    /** Creates a new float field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun float(name: String, callback: FieldSchemaBuilder.() -> Unit = {}): DocumentSchemaBuilder = apply {
        fields += FieldSchemaBuilder.new(name, FieldType.FLOAT, callback).build()
    }

    /** Creates a new double field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun double(name: String, callback: FieldSchemaBuilder.() -> Unit = {}): DocumentSchemaBuilder = apply {
        fields += FieldSchemaBuilder.new(name, FieldType.DOUBLE, callback).build()
    }

    /** Builds the document.
     */
    fun build(): DocumentSchema {
        return DocumentSchema.new(
            namespace = namespace,
            languages = Language.entries,
            fields = fields
        )
    }
}
