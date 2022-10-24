package be.rlab.search

import be.rlab.search.model.Document
import be.rlab.search.model.DocumentSchema
import be.rlab.search.model.FieldSchema
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class DocumentDataBinder {
    fun<T : Any> bind(source: Document, type: KClass<T>): T {
        val schema: DocumentSchema<T> = DocumentSchema.fromClass(type)
        val constructor = type.primaryConstructor
            ?: throw RuntimeException("no primary constructor found")
        val values: List<Any?> = constructor.parameters.map { param ->
            val field = findFieldByPropertyName(schema, requireNotNull(param.name))
            val value: Any? = field?.let { source[field.name] }
            require(param.type.isMarkedNullable || value != null) { "field value cannot be null" }
            value
        }
        return constructor.call(*values.toTypedArray())
    }

    inline fun<reified T : Any> bind(source: Document): T {
        return bind(source, T::class)
    }

    private fun findFieldByPropertyName(
        schema: DocumentSchema<*>,
        propertyName: String
    ): FieldSchema? {
        return schema.fields.find { field -> field.propertyName == propertyName }
    }
}
