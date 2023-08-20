package be.rlab.search.mapper

import be.rlab.search.model.Document
import be.rlab.search.model.DocumentSchema
import be.rlab.search.model.FieldSchema
import be.rlab.search.model.FieldType
import be.rlab.search.schema.FieldSchemaBuilder
import kotlin.reflect.KType

class SimpleTypeMapper : FieldTypeMapper {
    private val supportedTypes = listOf(
        String::class, Int::class, Long::class, Double::class, Float::class
    )

    override fun supports(sourceType: KType): Boolean {
        return supportedTypes.contains(sourceType.classifier)
    }

    override fun mapSchema(sourceType: KType, builder: FieldSchemaBuilder): List<FieldSchema> {
        require(supportedTypes.contains(sourceType.classifier)) {
            "source type not supported: $sourceType"
        }

        val fieldType = when (sourceType.classifier) {
            String::class -> FieldType.TEXT
            Int::class -> FieldType.INT
            Long::class -> FieldType.LONG
            Double::class -> FieldType.DOUBLE
            Float::class -> FieldType.FLOAT
            else -> throw RuntimeException(
                "Unsupported property type '${sourceType}' for field '${builder.name}'"
            )
        }
        val stored = builder.store ?: fieldType.stored
        val indexed = builder.index ?: fieldType.indexed

        require(stored || sourceType.isMarkedNullable) {
            "If the field will not be stored the property must be nullable: name=${builder.name}"
        }

        return listOf(
            builder
                .type(fieldType)
                .store(stored)
                .index(indexed)
                .build()
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> mapValue(targetType: KType, fieldName: String, schema: DocumentSchema, document: Document): T? {
        require(supportedTypes.contains(targetType.classifier)) {
            "target type not supported: $targetType"
        }

        val fieldSchema = schema.findField(fieldName)
        return fieldSchema?.let {
            document.getValues(fieldSchema.name)?.let { values: List<Any> ->
                values.first() as T
            }
        }
    }
}
