package be.rlab.training

import be.rlab.nlp.Normalizer
import be.rlab.nlp.SentimentAnalyzer.Companion.NAMESPACE
import be.rlab.nlp.SentimentAnalyzer.Companion.SENTIMENT_FIELD
import be.rlab.nlp.SentimentAnalyzer.Companion.SENTIMENT_NEG_FIELD
import be.rlab.nlp.SentimentAnalyzer.Companion.SENTIMENT_POS_FIELD
import be.rlab.nlp.SentimentAnalyzer.Companion.VALUE_FIELD
import be.rlab.nlp.model.Language.ENGLISH
import be.rlab.nlp.model.Language.SPANISH
import be.rlab.search.IndexManager
import be.rlab.search.DocumentBuilder
import be.rlab.support.csv.Field
import be.rlab.support.csv.ParserConfig
import java.io.File

class SentimentLoader(
    private val basePath: String,
    indexManager: IndexManager
) : DataSetLoader(indexManager) {

    companion object {
        private const val LEXICON: String = "lexicon"
        private const val TWEETS: String = "tweets"
        private val TABS: ParserConfig = ParserConfig.new("\t")
    }

    private val dataSets: List<DataSet> = listOf(
        DataSet(NAMESPACE, SPANISH, LEXICON, file("michigan-lexicons-medium.es.csv"), TABS),
        DataSet(NAMESPACE, SPANISH, LEXICON, file("michigan-lexicons-full.es.csv"), TABS),
        DataSet(NAMESPACE, SPANISH, TWEETS, file("sentistrength-1600_tweets_dev_complete.es.csv"), TABS),
        DataSet(NAMESPACE, SPANISH, TWEETS, file("sentistrength-1600_tweets_test_average_complete.es.tsv"), TABS),
        DataSet(NAMESPACE, ENGLISH, TWEETS, file("michigan-tweets-complete.en.csv"))
    )

    private val parsers: Map<List<String>, DocumentBuilder.(List<Field>) -> Unit> = mapOf(
        listOf(
            "michigan-lexicons-medium.es.csv",
            "michigan-lexicons-full.es.csv"
        ) to { record ->
            val sentiment: String = record[2].value
            val value: String = Normalizer(
                text = record[0].value,
                language = SPANISH,
                removeStopWords = true,
                stemming = false
            ).normalize()

            text(VALUE_FIELD, value)

            when(sentiment) {
                "pos" -> addSentiment(negative = 1, positive = 2)
                "neg" -> addSentiment(negative = 2, positive = 1)
                else -> throw RuntimeException("unknown sentiment: $sentiment")
            }
        },
        listOf(
            "sentistrength-1600_tweets_dev_complete.es.csv",
            "sentistrength-1600_tweets_test_average_complete.es.tsv"
        ) to { record ->
            val value: String = Normalizer(
                text = record[2].value,
                language = SPANISH,
                removeStopWords = true,
                stemming = false
            ).normalize()

            text(VALUE_FIELD, value)
            addSentiment(negative = record[1].value.toInt(), positive = record[0].value.toInt())
        },
        listOf("michigan-tweets-complete.en.csv") to { record ->
            val sentiment: Int = record[1].value.toInt()
            val value: String = record[3].value
            text(VALUE_FIELD, value)

            when(sentiment) {
                0 -> addSentiment(negative = 2, positive = 1)
                1 -> addSentiment(negative = 1, positive = 2)
                else -> throw RuntimeException("unknown sentiment: $sentiment")
            }
        }
    )

    fun loadDataIfRequired() {
        dataSets.forEach { dataSet ->
            loadIfRequired(dataSet) { record ->
                val parser: DocumentBuilder.(List<Field>) -> Unit = parsers.filterKeys { dataSets ->
                    dataSets.contains(dataSet.file.name)
                }.map {
                    it.value
                }.single()

                indexManager.index(dataSet.namespace, dataSet.language) {
                    try {
                        parser(record)
                    } catch (cause: Exception) {
                        println("error parsing record: $record")
                    }
                }
            }
        }
    }

    private fun DocumentBuilder.addSentiment(
        negative: Int,
        positive: Int
    ) {
        int(SENTIMENT_FIELD, negative, positive)
        int(SENTIMENT_NEG_FIELD, negative) {
            store()
        }
        int(SENTIMENT_POS_FIELD, positive) {
            store()
        }
    }

    private fun file(name: String): File {
        return File(basePath, name)
    }
}
