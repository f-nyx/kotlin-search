package be.rlab.search.schema

import be.rlab.search.annotation.IndexField
import be.rlab.search.annotation.IndexFieldType
import be.rlab.search.annotation.Indexed
import be.rlab.search.annotation.Stored
import be.rlab.search.mapper.FieldTypeMapper
import be.rlab.search.model.BoolValue
import be.rlab.search.model.FieldSchema
import be.rlab.search.model.FieldType
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

/** Builder to create a [FieldSchema].
 */
class FieldSchemaBuilder private constructor (
    name: String
){
    companion object {
        fun new(
            name: String,
            type: FieldType,
            callback: FieldSchemaBuilder.() -> Unit
        ): FieldSchemaBuilder =
            FieldSchemaBuilder(name).type(type).apply(callback)

        /** Creates a new field linked to a property.
         * @param property Property to link to this field.
         * @return the new field.
         */
        fun buildFromProperty(
            property: KProperty1<Any, *>,
            fieldTypeMappers: List<FieldTypeMapper>
        ): List<FieldSchema> {
            val field: IndexField = property.findAnnotation()
                ?: throw RuntimeException("@IndexField annotation not found")
            val typeMetadata: IndexFieldType? = property.findAnnotation()
            val name = field.name.takeIf { it.isNotBlank() }
                ?: field.fieldName.takeIf { it.isNotBlank() }
                ?: property.name
            val mapper = fieldTypeMappers.firstOrNull { mapper -> mapper.supports(property.returnType) }
                ?: throw RuntimeException("no type mapper found for property: $name")
            val stored: Boolean? = property.findAnnotation<Stored>()?.value
                ?: if (field.store == BoolValue.DEFAULT) null else field.store == BoolValue.YES
            val indexed: Boolean? = property.findAnnotation<Indexed>()?.value
                ?: if (field.index == BoolValue.DEFAULT) null else field.index == BoolValue.YES
            val docValues: Boolean = field.docValues

            val builder = FieldSchemaBuilder(name)
                .propertyName(property.name)
                .type(typeMetadata?.type)
                .store(stored)
                .index(indexed)
                .docValues(docValues)
            return mapper.mapSchema(property.returnType, builder)
        }
    }

    var propertyName: String? = null
        private set
    var name: String = name
        private set
    var type: FieldType? = null
        private set
    var store: Boolean? = null
        private set
    var index: Boolean? = null
        private set
    var docValues: Boolean = false
        private set

    /** Sets the Kotlin property name, if it applies.
     * This is only used by the IndexMapper.
     * @param name Kotlin object property name.
     */
    fun propertyName(name: String): FieldSchemaBuilder = apply {
        propertyName = name
    }

    /** Sets the Lucene field name.
     * @param fieldName Lucene field name.
     */
    fun name(fieldName: String): FieldSchemaBuilder = apply {
        name = fieldName
    }

    /** Sets the Lucene type of this field.
     * @param fieldType Lucene type.
     */
    fun type(fieldType: FieldType?): FieldSchemaBuilder = apply {
        type = fieldType
    }

    /** Configures whether the value of this field must be stored in the index or not.
     * @param stored True to store, false to prevent from storing the field.
     */
    fun store(stored: Boolean? = true): FieldSchemaBuilder = apply {
        store = stored
    }

    /** Configures whether this field must be indexed or not.
     * @param indexed True to index, false to prevent from indexing the field.
     */
    fun index(indexed: Boolean? = true): FieldSchemaBuilder = apply {
        index = indexed
    }

    /** Marks this field to be stored as DocValues.
     * DocValues are a document-level fields, and they are much faster for sorting and faceting.
     *
     * @param isDocValues true to store this field as DocValues, false otherwise.
     *
     * @see https://solr.apache.org/guide/6_6/docvalues.html
     */
    fun docValues(isDocValues: Boolean = true): FieldSchemaBuilder = apply {
        docValues = isDocValues
    }

    /** Builds the field schema.
     */
    fun build(): FieldSchema {
        val resolvedType = requireNotNull(type) { "The Lucene type is required and it is not set." }

        return FieldSchema.new(
            name = name,
            type = resolvedType,
            stored = store ?: resolvedType.stored,
            indexed = index ?: resolvedType.indexed,
            docValues = docValues,
            propertyName = propertyName
        )
    }
}
