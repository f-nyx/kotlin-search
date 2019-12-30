package be.rlab.kotlin.search.nlp

import be.rlab.kotlin.search.model.Language
import org.tartarus.snowball.SnowballProgram
import org.tartarus.snowball.ext.EnglishStemmer
import org.tartarus.snowball.ext.PortugueseStemmer
import org.tartarus.snowball.ext.SpanishStemmer

/** Tartarus snowball stemmer wrapper.
 *
 * It supports all languages defined in the [Language] enumeration.
 */
class SnowballStemmer(
    private val stemmer: SnowballProgram
) {

    companion object {
        private val stemmers: Map<Language, SnowballProgram> = mapOf(
            Language.ENGLISH to EnglishStemmer(),
            Language.SPANISH to SpanishStemmer(),
            Language.PORTUGUESE to PortugueseStemmer()
        )

        /** Creates a new stemmer for the specified language.
         * @param language Stemmer language.
         * @return the required stemmer.
         */
        fun new(language: Language): SnowballStemmer {
            val stemmer: SnowballProgram = stemmers[language]
                ?: throw RuntimeException("stemmer for language $language not supported")

            return SnowballStemmer(stemmer)
        }
    }

    /** Applies the stemmer to a text.
     * @param text Text to apply the stemmer.
     * @return the stemmed text.
     */
    fun stem(text: String): String {
        stemmer.current = text
        stemmer.stem()
        return stemmer.current
    }
}