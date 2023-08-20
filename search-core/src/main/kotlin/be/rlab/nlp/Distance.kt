package be.rlab.nlp

import org.apache.lucene.search.spell.JaroWinklerDistance
import org.apache.lucene.search.spell.LevenshteinDistance
import org.apache.lucene.search.spell.NGramDistance

/** Utility to calculate text distance using different algorithms.
 */
object Distance {

    /** Calculates the Jaro-Winkler distance between two texts.
     *
     * This algorithm works very well on terms that share the same prefixes.
     *
     * @param text A text to measure.
     * @param otherText Other text to measure.
     * @return the distance as a float between 0 and 1.
     */
    fun jaroWinkler(
        text: String,
        otherText: String
    ): Float {
        return JaroWinklerDistance().getDistance(text, otherText)
    }

    /** Calculates the Damerau–Levenshtein distance between two texts.
     *
     * This algorithm works fine to detect misspellings.
     *
     * @param text A text to measure.
     * @param otherText Other text to measure.
     * @return the distance as a float between 0 and 1.
     */
    fun levenshtein(
        text: String,
        otherText: String
    ): Float {
        return LevenshteinDistance().getDistance(text, otherText)
    }

    /** Calculates the N-gram distance between two texts.
     *
     * It works well for predicting the next token in a text.
     *
     * @param text A text to measure.
     * @param otherText Other text to measure.
     * @param size Size of the N-Grams groups.
     * @return the distance as a float between 0 and 1.
     */
    fun ngram(
        text: String,
        otherText: String,
        size: Int = 2
    ): Float {
        return NGramDistance(size).getDistance(text, otherText)
    }
}
