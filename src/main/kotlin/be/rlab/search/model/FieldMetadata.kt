package be.rlab.search.model

/** Represents the field metadata stored in the index.
 */
data class FieldMetadata(
    val type: FieldType,
    val stored: Boolean,
    val indexed: Boolean,
    val docValues: Boolean
) {
    companion object {
        fun deserialize(metadata: String): FieldMetadata {
            val entries = metadata.split(",").associate { field ->
                val entry = field.split("=")
                entry[0] to entry[1]
            }
            return FieldMetadata(
                type = FieldType.valueOf(entries.getValue("type")),
                stored = entries.getValue("stored") == "1",
                indexed = entries.getValue("indexed") == "1",
                docValues = entries.getValue("docValues") == "1"
            )
        }

        private fun mapBool(value: Boolean): String {
            return if (value) "1" else "0"
        }
    }

    fun serialize(): String {
        return "type=${type.name},stored=${mapBool(stored)},indexed=${mapBool(indexed)},docValues=${mapBool(docValues)}"
    }
}
