package be.rlab.search.model

/** Contains metadata to manage a single field in the index.
 * The options in this schema overrides the default options from the type.
 */
data class FieldSchema(
    val propertyName: String?,
    val name: String,
    val type: FieldType,
    val stored: Boolean,
    val indexed: Boolean,
    val docValues: Boolean
) {
    companion object {
        /** Creates a new field not linked to a property.
         * @param name Field name.
         * @param type Field type.
         * @param stored True to store the field, false otherwise.
         * @param indexed True to index the field, false otherwise.
         * @param propertyName Kotlin's property name related to this field, if it applies.
         */
        fun new(
            name: String,
            type: FieldType,
            stored: Boolean,
            indexed: Boolean,
            docValues: Boolean,
            propertyName: String?
        ): FieldSchema = FieldSchema(
            name = name,
            type = type,
            indexed = indexed,
            stored = stored,
            docValues = docValues,
            propertyName = propertyName
        )

        fun validate(name: String, type: FieldType, values: List<*>) {
            require(values.isNotEmpty()) { "the field must have a value: name=$name" }

            when (type) {
                FieldType.TEXT, FieldType.STRING ->
                    require(values.all { it is String }) { "one or more values are not String: name=$name" }
                FieldType.INT ->
                    require(values.all { it is Int }) { "one or more values are not Int: name=$name" }
                FieldType.LONG ->
                    require(values.all { it is Long }) { "one or more values are not Long: name=$name" }
                FieldType.FLOAT ->
                    require(values.all { it is Float }) { "one or more values are not Float: name=$name" }
                FieldType.DOUBLE ->
                    require(values.all { it is Double }) { "one or more values are not Double: name=$name" }
            }
        }
    }

    fun validate(values: List<*>): FieldSchema = apply {
        validate(name, type, values)
    }
}
