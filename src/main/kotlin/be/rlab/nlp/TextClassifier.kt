package be.rlab.nlp

import be.rlab.nlp.Distance.jaroWinkler
import be.rlab.nlp.model.ClassificationResult
import be.rlab.nlp.model.Language
import be.rlab.nlp.model.TrainingDataSet
import be.rlab.search.IndexManager
import be.rlab.search.LuceneIndex
import be.rlab.search.model.SearchResult
import be.rlab.search.query.wildcard

/** This class allows to train and query text classifiers.
 *
 * This implementation is not designed for performance. The classification retrieves all features from the index
 * for a specific namespace and it evaluates distances on runtime. The features are normalized before storing them.
 *
 * It uses an [LuceneIndex] to store the training data set.
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

        indexManager.index(namespace, language) {
            text(CATEGORY_FIELD, category)
            text(TEXT_FIELD, normalizedText)
        }
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

        val features: SearchResult = indexManager.search(namespace, language, limit = MAX_FEATURES) {
            wildcard(CATEGORY_FIELD, "*")
        }

        val normalizedText: String = Normalizer(text, language)
            .applyStemming()
            .removeStopWords()
            .normalize()

        return features.docs.groupBy { document ->
            val value: String = document[CATEGORY_FIELD]!!
            value
        }.map { (category, documents) ->
            val distance: Float = documents.map { document ->
                jaroWinkler(document[TEXT_FIELD]!!, normalizedText)
            }.maxOrNull() ?: 0.0F

            ClassificationResult(
                assignedClass = category,
                score = distance.toDouble()
            )
        }
    }
}
