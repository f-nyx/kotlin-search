package be.rlab.support

import java.io.BufferedReader

object ResourceLoader {
    fun fromClasspath(path: String): BufferedReader {
        return Thread.currentThread().contextClassLoader
            .getResourceAsStream(path)?.bufferedReader()
            ?: throw RuntimeException("Resource not found: $path")
    }
}