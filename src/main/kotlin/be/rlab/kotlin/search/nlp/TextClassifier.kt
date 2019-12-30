package be.rlab.kotlin.search.nlp

import be.rlab.augusto.nlp.model.*
import be.rlab.kotlin.search.IndexManager
import be.rlab.kotlin.search.model.*
import org.apache.lucene.search.spell.JaroWinklerDistance

/** This class allows to train and query text classifiers.
 *
 * This implementation is not designed for performance. The classification retrieves all features from the index
 * for a specific namespace and it evaluates distances on runtime. The features are normalized before storing them.
 *
 * It uses an [Index] to store the training data set.
 */
class TextClassifier(
    private val indexManager: IndexManager,
    private val namespace: String
) {

    companion object {
        private const val CATEGORY_FIELD: String = "category"
        private const val TEXT_FIELD: String = "text"
        private const val MAX_FEATURES: Int = 10000
    }

    /** Analyzes and sets the category for a text.
     * It stores the text and the category into the index.
     *
     * @param category Text category.
     * @param text Text to assign the specified category.
     * @param language Text language.
     */
    fun train(
        category: String,
        text: String,
        language: Language
    ) {
        val normalizedText: String = Normalizer(text, language = language)
            .applyStemming()
            .removeStopWords()
            .normalize()

        indexManager.index(
            Document.new(
                namespace, language,
                Field.text(CATEGORY_FIELD, category),
                Field.text(TEXT_FIELD, normalizedText)
            )
        )
    }

    /** Trains the classifier from data sets.
     * @param dataSets Data sets used to train this classifier.
     */
    fun train(dataSets: List<TrainingDataSet>) {
        dataSets.forEach { dataSet ->
            dataSet.categories.forEach { category ->
                dataSet.values.forEach { value ->
                    train(category, value, dataSet.language)
                }
            }
        }
    }

    /** Resolves the top category for a text.
     * @param text Text to resolve category.
     * @param language Text language.
     * @return the resolved category or null if there's no matching category.
     */
    fun classify(
        text: String,
        language: Language
    ): String? {
        return classifyAll(text, language).firstOrNull()?.assignedClass
    }

    /** Resolves all categories for a text.
     * @param text Text to search categories for.
     * @param language Text language.
     * @return the list of matching categories.
     */
    fun classifyAll(
        text: String,
        language: Language
    ): List<ClassificationResult> {

        val features: PaginatedResult = indexManager.search(
            namespace = namespace,
            fields = mapOf(CATEGORY_FIELD to "*"),
            language = language,
            limit = MAX_FEATURES
        )

        val normalizedText: String = Normalizer(text, language)
            .applyStemming()
            .removeStopWords()
            .normalize()

        return features.results.groupBy { document ->
            document[CATEGORY_FIELD]!!
        }.map { (category, documents) ->
            val distance: Float = documents.map { document ->
                distance(document[TEXT_FIELD]!!, normalizedText)
            }.max() ?: 0.toFloat()

            ClassificationResult(
                assignedClass = category,
                score = distance.toDouble()
            )
        }
    }

    /** Calculates the Jaro-Winkler distance between two texts.
     * This algorithm works very well on terms that share the same prefixes.
     * @param text A text to measure.
     * @param otherText Other text to measure.
     * @return the distance as a float between 0 and 1.
     */
    fun distance(
        text: String,
        otherText: String
    ): Float {
        return JaroWinklerDistance().getDistance(text, otherText)
    }
}
