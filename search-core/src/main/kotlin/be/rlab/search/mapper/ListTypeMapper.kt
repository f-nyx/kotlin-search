package be.rlab.search.mapper

import be.rlab.search.model.Document
import be.rlab.search.model.DocumentSchema
import be.rlab.search.model.FieldSchema
import be.rlab.search.model.FieldType
import be.rlab.search.schema.FieldSchemaBuilder
import kotlin.reflect.KType

class ListTypeMapper : FieldTypeMapper {
    override fun supports(sourceType: KType): Boolean {
        return sourceType.classifier == List::class
    }

    override fun mapSchema(sourceType: KType, builder: FieldSchemaBuilder): List<FieldSchema> {
        require(sourceType.classifier == List::class) { "source type not supported: $sourceType" }
        require(sourceType.arguments.isNotEmpty()) { "list type cannot be resolved" }
        require(!sourceType.isMarkedNullable) { "List type cannot be null" }

        val fieldType = when (sourceType.arguments.first().type?.classifier) {
            String::class -> FieldType.TEXT
            Int::class -> FieldType.INT
            Long::class -> FieldType.LONG
            Double::class -> FieldType.DOUBLE
            Float::class -> FieldType.FLOAT
            else -> throw RuntimeException(
                "Unsupported property type '${sourceType}' for field '${builder.name}'"
            )
        }

        return listOf(builder.type(fieldType).build())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> mapValue(targetType: KType, fieldName: String, schema: DocumentSchema, document: Document): T? {
        require(targetType.classifier == List::class) { "target type not supported: $targetType" }
        val fieldSchema = schema.findField(fieldName)
        return fieldSchema?.let {
            document.getValues(fieldSchema.name) as T
        }
    }
}
