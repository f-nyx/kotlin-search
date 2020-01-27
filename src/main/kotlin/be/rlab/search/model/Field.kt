package be.rlab.search.model

data class Field(
    val name: String,
    val value: Any,
    val type: FieldType,
    val stored: Boolean = type.stored,
    val indexed: Boolean = type.indexed
) {
    fun index(indexed: Boolean = true): Field = copy(
        indexed = indexed
    )

    fun store(stored: Boolean = true): Field = copy(
        stored = stored
    )
}
