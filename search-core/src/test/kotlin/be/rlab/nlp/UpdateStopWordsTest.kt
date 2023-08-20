package be.rlab.nlp

import be.rlab.nlp.model.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException
import java.net.URI

/** This test downloads all stop words files from the stopwords-iso repository and updates
 * the files in `nlp/stopwords/`. It will not fail if a language is not supported by the
 * stopwords-iso project.
 *
 * Look at the github repository for further information: https://github.com/stopwords-iso/stopwords-iso
 */
@Disabled
class UpdateStopWordsTest {
    companion object {
        const val DOWNLOAD_URL: String =
            "https://raw.githubusercontent.com/stopwords-iso/stopwords-{code}/master/stopwords-{code}.txt"
    }

    @Test
    fun update() {
        Language.values().forEach { language ->
            try {
                println("updating stop words for language: $language")
                val url = URI.create(DOWNLOAD_URL.replace("{code}", language.code)).toURL()
                val data = url.openStream().bufferedReader().readText()
                val file = File("src/main/resources/nlp/stopwords/${language.name.lowercase()}.txt")
                file.writeText(data)
            } catch(cause: FileNotFoundException) {
                println("stop words not supported for language: $language")
            }
        }
    }
}
