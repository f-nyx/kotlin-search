package be.rlab.support.csv

import java.nio.charset.Charset

data class Field(
    val data: ByteArray
) {
    val value: String by lazy {
        data.toString(Charset.defaultCharset())
    }
}
