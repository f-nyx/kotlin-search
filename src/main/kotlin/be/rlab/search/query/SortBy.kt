package be.rlab.search.query

import be.rlab.search.model.FieldType
import be.rlab.search.model.QueryBuilder
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortedNumericSortField
import kotlin.reflect.KProperty1

fun QueryBuilder.sortBy(
    vararg fieldsNames: String,
    reverse: Boolean = false
): QueryBuilder = apply {
    fieldsNames
        .map { name -> requireNotNull(getFieldSchema(name)) { "field schema not found: name=$name" } }
        .forEach { fieldSchema ->
            require(fieldSchema.docValues) { "The field must be stored as docValues to enable sorting." }
            val sortField = when (fieldSchema.type) {
                FieldType.TEXT, FieldType.STRING -> SortField(fieldSchema.name, SortField.Type.STRING, reverse)
                FieldType.INT -> SortedNumericSortField(fieldSchema.name, SortField.Type.INT, reverse)
                FieldType.LONG -> SortedNumericSortField(fieldSchema.name, SortField.Type.LONG, reverse)
                FieldType.FLOAT -> SortedNumericSortField(fieldSchema.name, SortField.Type.FLOAT, reverse)
                FieldType.DOUBLE -> SortedNumericSortField(fieldSchema.name, SortField.Type.DOUBLE, reverse)
            }
            addSortField(sortField)
        }
}

fun<T : Any> QueryBuilder.sortBy(
    vararg properties: KProperty1<T, *>,
    reverse: Boolean = false
): QueryBuilder = apply {
    sortBy(*properties.map { property -> property.name }.toTypedArray(), reverse = reverse)
}
