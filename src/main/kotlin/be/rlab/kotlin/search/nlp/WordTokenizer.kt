package be.rlab.kotlin.search.nlp

import be.rlab.kotlin.search.model.Token
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.standard.StandardTokenizer
import java.io.Reader

/** Splits out a document into words.
 * It removes spaces and punctuation.
 * @param punctuation true to remove punctuation, false otherwise.
 */
class WordTokenizer(
    private val punctuation: Boolean = true
) : Tokenizer {

    override fun stream(document: Reader): TokenStream {
        return if (punctuation) {
            StandardTokenizer().apply {
                setReader(document)
            }
        } else {
            WhitespaceTokenizer().apply {
                setReader(document)
            }
        }
    }

    override fun tokenize(document: Reader): List<Token> {
        return Tokenizers.tokenize(stream(document))
    }
}
