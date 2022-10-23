package be.rlab.nlp

import be.rlab.nlp.model.Language
import be.rlab.nlp.model.Sentiment
import be.rlab.nlp.model.SentimentResult
import be.rlab.search.IndexManager
import be.rlab.search.query.term
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Analyzer that uses a pre-trained index to detect sentiments.
 *
 * The training set must be initialized by the [be.rlab.training.SentimentLoader].
 */
class SentimentAnalyzer(
    private val indexManager: IndexManager
) {
    companion object {
        const val NAMESPACE: String = "sentiments"
        const val VALUE_FIELD: String = "value"
        const val SENTIMENT_FIELD: String = "sentiment"
        const val SENTIMENT_NEG_FIELD: String = "positive"
        const val SENTIMENT_POS_FIELD: String = "negative"
    }

    /** Analyzes a text to determine the average sentiment of the text.
     *
     * @param text Text to analyze.
     * @param language Text language.
     * @return returns the sentiment analysis result.
     */
    fun analyze(
        text: String,
        language: Language
    ): SentimentResult {
        val words: Map<String, Int> = Normalizer(
            text = text,
            language = language,
            removeStopWords = true,
            stemming = false
        ).normalize().split(" ").map { word ->
            val score: Int = indexManager.find(NAMESPACE, language) {
                term(VALUE_FIELD, word)
            }.fold(0) { score, doc ->
                val positive: Int = doc[SENTIMENT_POS_FIELD]!!
                val negative: Int = doc[SENTIMENT_NEG_FIELD]!!

                score + positive - negative
            }

            word to score
        }.toMap()
        val negativeCount: Int = words.values.count { value ->
            value < 0
        }
        val positiveCount: Int = words.values.count { value ->
            value > 0
        }
        val negativeScore: Int = words.values.filter { value ->
            value < 0
        }.fold(0) { score, value ->
            score + abs(value)
        }
        val positiveScore: Int = words.values.filter { value ->
            value > 0
        }.fold(0) { score, value ->
            score + abs(value)
        }

        val size: Float = words.size.toFloat()
        val count: Float = max(negativeCount, positiveCount).toFloat()
        val score: Float = min(size, count + max(negativeScore, positiveScore).toFloat())

        return SentimentResult(
            score = (1 / (size / score)),
            sentiment = when {
                positiveScore > negativeScore -> Sentiment.POSITIVE
                positiveScore < negativeScore -> Sentiment.NEGATIVE
                else -> Sentiment.UNKNOWN
            }
        )
    }
}
