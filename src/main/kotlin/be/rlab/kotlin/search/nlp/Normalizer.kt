package be.rlab.kotlin.search.nlp

import be.rlab.kotlin.search.model.Language
import java.text.Normalizer as JavaNormalizer

/** String normalizer.
 *
 * By default it removes diacritics, applies the stemmer for the specified language, converts all terms to
 * lowercase, and joins the terms with a single space.
 *
 * @param text Text to normalize.
 * @param language Text language.
 * @param caseSensitive Indicates whether to convert string to lowercase or not.
 * @param form Normalization form.
 * @param removeDiacritics Indicates whether to remove diacritics.
 * @param removePunctuation Indicates whether to split text into words.
 * @param removeStopWords Indicates whether to strip out stop words.
 * @param stemming Indicates whether to apply the stemmer to each term.
 * @param joinWith String to join the terms.
 */
data class Normalizer(
    private val text: String,
    private val language: Language,
    private val caseSensitive: Boolean = false,
    private val form: JavaNormalizer.Form = JavaNormalizer.Form.NFD,
    private val removeDiacritics: Boolean = true,
    private val removePunctuation: Boolean = true,
    private val removeStopWords: Boolean = false,
    private val stemming: Boolean = true,
    private val joinWith: String = " "
) {
    companion object {
        private val REGEX_UNACCENT: Regex = Regex("\\p{InCombiningDiacriticalMarks}+")

        /** Creates a new normalizer for the specified text.
         * @param text Text to normalize.
         * @param language Text language.
         * @return a new normalizer.
         */
        fun new(
            text: String,
            language: Language
        ): Normalizer =
            Normalizer(
                text = text,
                language = language
            )
    }

    /** Word tokenizer to split text into words. */
    private val wordTokenizer = WordTokenizer()
    private val stopWordTokenizer =
        StopWordTokenizer.new(language)
    private val stemmer = SnowballStemmer.new(language)

    fun caseSensitive(): Normalizer = copy(
        caseSensitive = true
    )

    fun caseInsensitive(): Normalizer = copy(
        caseSensitive = false
    )

    fun form(form: JavaNormalizer.Form): Normalizer = copy(
        form = form
    )

    fun removeDiacritics(): Normalizer = copy(
        removeDiacritics = true
    )

    fun keepDiacritics(): Normalizer = copy(
        removeDiacritics = false
    )

    fun removeStopWords(): Normalizer = copy(
        removeStopWords = true
    )

    fun keepStopWords(): Normalizer = copy(
        removeStopWords = false
    )

    fun removePunctuation(): Normalizer = copy(
        removePunctuation = true
    )

    fun keepPunctuation(): Normalizer = copy(
        removePunctuation = false
    )

    fun applyStemming(): Normalizer = copy(
        stemming = true
    )

    fun skipStemming(): Normalizer = copy(
        stemming = false
    )

    fun joinWith(joinText: String): Normalizer = copy(
        joinWith = joinText
    )

    /** Applies normalizations and returns the normalized text.
     * @return a valid text.
     */
    fun normalize(): String {
        var normalizedText = with(JavaNormalizer.normalize(text, form)) { ->
            if (removeDiacritics) {
                replace(REGEX_UNACCENT, "")
            } else {
                this
            }
        }

        if (removeDiacritics) {
            normalizedText = normalizedText.replace(REGEX_UNACCENT, "")
        }
        if (removePunctuation) {
            normalizedText = wordTokenizer.tokenize(normalizedText.reader()).map { word ->
                word.toString()
            }.joinToString(joinWith)
        }
        if (removeStopWords) {
            normalizedText = stopWordTokenizer.tokenize(normalizedText.reader()).map { word ->
                word.toString()
            }.joinToString(joinWith)
        }
        if (stemming) {
            normalizedText = normalizedText.split(joinWith).joinToString(joinWith) { word ->
                stemmer.stem(word)
            }
        }
        if (!caseSensitive) {
            normalizedText = normalizedText.toLowerCase()
        }

        return normalizedText
    }
}
