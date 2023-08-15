package be.rlab.search.mapper

import be.rlab.search.model.Document
import be.rlab.search.model.DocumentSchema
import be.rlab.search.model.FieldSchema
import be.rlab.search.schema.FieldSchemaBuilder
import kotlin.reflect.KType

/** Implement this interface to map from Kotlin types to Lucene fields and viceversa.
 */
interface FieldTypeMapper {
    /** Indicates whether this mapper can convert a Kotlin type.
     * @param sourceType Type to verify.
     * @return true if this mapper can convert the source type, false otherwise.
     */
    fun supports(sourceType: KType): Boolean

    /** Converts a Kotlin type to a list of fields schemas.
     * If the source type is not supported, it throws an error.
     */
    fun mapSchema(sourceType: KType, builder: FieldSchemaBuilder): List<FieldSchema>

    /** Converts a field or a set of fields from a Lucene document into its Kotlin value.
     * If the target type is not supported, it throws an error.
     */
    fun<T> mapValue(
        targetType: KType,
        fieldName: String,
        schema: DocumentSchema,
        document: Document
    ): T?
}
