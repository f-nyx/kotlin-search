package be.rlab.nlp.model

/** Represents the result of sentiment analysis.
 *
 * The score indicates how strong is the sentiment within the analyzed text.
 */
data class SentimentResult(
    /** Number between 0 and 1 that indicates how strong is the sentiment. */
    val score: Float,

    /** Resolved sentiment. */
    val sentiment: Sentiment
)
