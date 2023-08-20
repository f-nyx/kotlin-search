package be.rlab.nlp

import be.rlab.nlp.model.Language
import be.rlab.nlp.model.Token
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.core.StopFilter
import org.apache.lucene.analysis.standard.StandardTokenizer
import java.io.Reader
import kotlin.streams.toList

/** Tokenizer to remove stop words from a document.
 * By default it uses the stop words list embedded in this package.
 */
class StopWordTokenizer(
    private val stopWords: List<String>
) : Tokenizer {

    companion object {
        /** Language to determine the list of stop words to use.
         * @param language Stop words language.
         * @return the new tokenizer.
         */
        fun new(language: Language): StopWordTokenizer {
            return StopWordTokenizer(
                stopWords(
                    language
                )
            )
        }

        /** Returns the list of default stop words for a language.
         * @param language Stop words language.
         * @return the list of stop words.
         */
        fun stopWords(language: Language): List<String> {
            val langFile = "nlp/stopwords/${language.name.lowercase()}.txt"

            return Thread.currentThread().contextClassLoader
                .getResourceAsStream(langFile)?.use { resource ->
                    resource.bufferedReader().lines().toList()
                } ?: emptyList()
        }
    }

    override fun stream(document: Reader): TokenStream {
        val wordTokenizer = StandardTokenizer().apply {
            setReader(document)
        }
        return StopFilter(wordTokenizer, CharArraySet(stopWords, false))
    }

    override fun tokenize(document: Reader): List<Token> {
        return Tokenizers.tokenize(stream(document))
    }
}
