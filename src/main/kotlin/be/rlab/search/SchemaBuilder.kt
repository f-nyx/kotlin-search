package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.model.DocumentSchema
import be.rlab.search.model.FieldSchema
import be.rlab.search.model.FieldType

/** Builder to define a [DocumentSchema].
 */
class SchemaBuilder private constructor (
    private val namespace: String
){
    companion object {
        fun new(
            namespace: String,
            callback: SchemaBuilder.() -> Unit
        ): SchemaBuilder =
            SchemaBuilder(namespace).apply(callback)
    }

    private val fields: MutableList<FieldSchema> = mutableListOf()

    /** Defines a new text field.
     *
     * By default text fields are indexed and stored.
     *
     * @param name Field name.
     */
    fun text(name: String): SchemaBuilder = apply {
        fields += FieldSchema.new(name, FieldType.TEXT)
    }

    /** Creates a new string field.
     *
     * String fields are saved as single terms and they're not indexed.
     * By default String fields are stored.
     *
     * @param name Field name.
     */
    fun string(name: String): SchemaBuilder = apply {
        fields += FieldSchema.new(name, FieldType.STRING)
    }

    /** Creates a new int field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun int(name: String): SchemaBuilder = apply {
        fields += FieldSchema.new(name, FieldType.INT)
    }

    /** Creates a new long field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun long(name: String): SchemaBuilder = apply {
        fields += FieldSchema.new(name, FieldType.LONG)
    }

    /** Creates a new float field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun float(name: String): SchemaBuilder = apply {
        fields += FieldSchema.new(name, FieldType.FLOAT)
    }

    /** Creates a new double field.
     * By default numeric fields are not stored.
     *
     * @param name Field name.
     */
    fun double(name: String): SchemaBuilder = apply {
        fields += FieldSchema.new(name, FieldType.DOUBLE)
    }

    /** Builds the document.
     */
    fun build(): DocumentSchema<*> {
        return DocumentSchema.new<Any>(
            namespace = namespace,
            languages = Language.values().toList(),
            fields = fields
        )
    }
}
