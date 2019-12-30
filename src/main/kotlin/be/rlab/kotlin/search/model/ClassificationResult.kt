package be.rlab.kotlin.search.model

/** Represents a text classification result.
 * It is used by the [be.rlab.augusto.nlp.TextClassifier] to retrieve all matching categories and
 * its scores for a text.
 *
 * @param assignedClass Text category.
 * @param score Score within the result set.
 */
data class ClassificationResult(
    val assignedClass: String,
    val score: Double
)
