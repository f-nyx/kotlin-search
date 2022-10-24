package be.rlab.search.model

import be.rlab.search.annotation.IndexField
import be.rlab.search.annotation.IndexFieldType
import be.rlab.search.annotation.Indexed
import be.rlab.search.annotation.Stored
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

data class FieldSchema(
    val propertyName: String?,
    val name: String,
    val type: FieldType,
    val stored: Boolean = type.stored,
    val indexed: Boolean = type.indexed
) {
    companion object {
        /** Creates a new field not linked to a property.
         * @param name Field name.
         * @param type Field type.
         */
        fun new(
            name: String,
            type: FieldType
        ): FieldSchema = FieldSchema(
            propertyName = null,
            name = name,
            type = type
        )

        /** Creates a new field linked to a property.
         * @param property Property to link to this field.
         * @return the new field.
         */
        fun from(property: KProperty1<Any, *>): FieldSchema {
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

            return FieldSchema(
                propertyName = property.name,
                name = field.fieldName.takeIf { it.isNotBlank() } ?: property.name,
                type = type,
                stored = stored,
                indexed = indexed || type.indexed
            )
        }
    }
}
