package be.rlab.search.model

data class Field<T>(
    val name: String,
    val values: List<T>,
    val type: FieldType,
    val stored: Boolean,
    val indexed: Boolean,
    val docValues: Boolean
) {
    companion object {
        fun<T> new(
            name: String,
            type: FieldType,
            values: List<T>,
            stored: Boolean = type.stored,
            indexed: Boolean = type.indexed,
            docValues: Boolean = false
        ): Field<T> = Field(
            name = name,
            type = type,
            values = values,
            stored = stored,
            indexed = indexed,
            docValues = docValues
        )
    }

    fun configure(
        stored: Boolean,
        indexed: Boolean,
        docValues: Boolean
    ): Field<T> = copy(
        stored = stored,
        indexed = indexed,
        docValues = docValues
    )

    fun addValues(newValues: List<T>): Field<T> = copy(
        values = values + newValues
    )
}
