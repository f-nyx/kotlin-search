package be.rlab.nlp

import be.rlab.nlp.model.Token
import org.apache.lucene.analysis.TokenStream
import java.io.Reader

/** Must be implemented by classes that split out a document into tokens.
 * A token is the minimum unit of text within a document stream.
 */
interface Tokenizer {
    /** Returns the Lucene token stream for the specified document.
     * @param document Document to tokenize.
     * @return the Lucene token stream.
     */
    fun stream(document: Reader): TokenStream

    /** Tokenizes a document and returns the list of result [Token]s.
     * @param document Document to tokenize.
     * @return the result tokens.
     */
    fun tokenize(document: Reader): List<Token>
}
