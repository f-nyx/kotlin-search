package be.rlab.search.model

import be.rlab.search.IndexField
import be.rlab.search.IndexFieldType
import be.rlab.search.Indexed
import be.rlab.search.Stored
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

data class FieldMetadata(
    val propertyName: String,
    val name: String,
    val type: FieldType,
    val stored: Boolean = type.stored,
    val indexed: Boolean = type.indexed
) {
    companion object {
        fun from(property: KProperty1<Any, *>): FieldMetadata {
            val field: IndexField = property.findAnnotation()
                ?: throw RuntimeException("@IndexField annotation not found")
            val typeMetadata: IndexFieldType? = property.findAnnotation()
            val indexed: Boolean = property.hasAnnotation<Indexed>()
            val stored: Boolean = property.findAnnotation<Stored>()?.value ?: true
            val type = typeMetadata?.type
                ?: when (property.returnType.classifier) {
                    String::class -> FieldType.TEXT
                    Int::class -> FieldType.INT
                    Long::class -> FieldType.LONG
                    Double::class -> FieldType.DOUBLE
                    Float::class -> FieldType.FLOAT
                    else -> throw RuntimeException(
                        "Unsupported property type '${property.returnType}' on property '${property.name}'"
                    )
                }

            require(stored || property.returnType.isMarkedNullable) {
                "If the field will not be stored the property must be nullable"
            }

            return FieldMetadata(
                propertyName = property.name,
                name = field.fieldName.takeIf { it.isNotBlank() } ?: property.name,
                type = type,
                stored = stored,
                indexed = indexed || type.indexed
            )
        }
    }
}
