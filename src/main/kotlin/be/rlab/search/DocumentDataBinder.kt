package be.rlab.search

import be.rlab.search.model.Document
import be.rlab.search.model.DocumentMetadata
import be.rlab.search.model.FieldMetadata
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class DocumentDataBinder {
    fun<T : Any> bind(source: Document, type: KClass<T>): T {
        val metadata: DocumentMetadata<T> = DocumentMetadata.read(type)
        val constructor = type.primaryConstructor
            ?: throw RuntimeException("no primary constructor found")
        val values: List<Any?> = constructor.parameters.map { param ->
            val field = findFieldByPropertyName(metadata, requireNotNull(param.name))
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
        metadata: DocumentMetadata<*>,
        propertyName: String
    ): FieldMetadata? {
        return metadata.fields.find { field -> field.propertyName == propertyName }
    }
}
