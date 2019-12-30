package be.rlab.kotlin.search.model

data class Field(
    val name: String,
    val value: String,
    val type: FieldType
) {
    companion object {
        fun string(
            name: String,
            value: String
        ): Field =
            Field(
                name = name,
                value = value,
                type = FieldType.STRING
            )

        fun text(
            name: String,
            value: String
        ): Field =
            Field(
                name = name,
                value = value,
                type = FieldType.TEXT
            )
    }
}
