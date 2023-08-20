package be.rlab.support.csv

data class ParserConfig(
    val bufferSize: Long,
    val separator: Byte
) {
    companion object {
        private const val DEFAULT_SEPARATOR: Byte = 44
        private const val DEFAULT_BUFFER_SIZE: Long = 1024 * 1024 * 50

        fun default(): ParserConfig = ParserConfig(
            bufferSize = DEFAULT_BUFFER_SIZE,
            separator = DEFAULT_SEPARATOR
        )

        fun new(
            separator: String,
            bufferSize: Long = DEFAULT_BUFFER_SIZE
        ): ParserConfig = ParserConfig(
            bufferSize = bufferSize,
            separator = separator[0].toByte()
        )
    }
}