package be.rlab.nlp

import be.rlab.nlp.model.Language
import java.text.Normalizer as JavaNormalizer

/** String normalizer.
 *
 * By default, it removes diacritics, removes punctuation, applies the stemmer for the specified language,
 * converts all terms to lowercase, and joins the terms with a single space.
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
class Normalizer(
    private val text: String,
    private var language: Language? = null,
    private var caseSensitive: Boolean = false,
    private var form: JavaNormalizer.Form = JavaNormalizer.Form.NFD,
    private var removeDiacritics: Boolean = true,
    private var removePunctuation: Boolean = true,
    private var removeStopWords: Boolean = false,
    private var stemming: Boolean = true,
    private var joinWith: String = " "
) {
    companion object {
        private val REGEX_UNACCENT: Regex = Regex("\\p{InCombiningDiacriticalMarks}+")

        /** Creates a new normalizer for the specified text.
         * @param text Text to normalize.
         * @param language Text language.
         * @return a new normalizer.
         */
        fun new(text: String, language: Language? = null): Normalizer = Normalizer(
            text = text,
            language = language
        )
    }

    fun forLanguage(newLanguage: Language): Normalizer = apply {
        language = newLanguage
    }

    fun caseSensitive(isCaseSensitive: Boolean = true): Normalizer = apply {
        caseSensitive = isCaseSensitive
    }

    fun caseInsensitive(isCaseSensitive: Boolean = false): Normalizer = apply {
        caseSensitive = isCaseSensitive
    }

    fun form(newForm: JavaNormalizer.Form): Normalizer = apply {
        form = newForm
    }

    fun removeDiacritics(remove: Boolean = true): Normalizer = apply {
        removeDiacritics = remove
    }

    fun keepDiacritics(keep: Boolean = true): Normalizer = apply {
        removeDiacritics = !keep
    }

    fun removeStopWords(remove: Boolean = true): Normalizer = apply {
        removeStopWords = remove
    }

    fun keepStopWords(keep: Boolean = true): Normalizer = apply {
        removeStopWords = !keep
    }

    fun removePunctuation(remove: Boolean = true): Normalizer = apply {
        removePunctuation = remove
    }

    fun keepPunctuation(keep: Boolean = true): Normalizer = apply {
        removePunctuation = !keep
    }

    fun applyStemming(apply: Boolean = true): Normalizer = apply {
        stemming = apply
    }

    fun skipStemming(): Normalizer = apply {
        stemming = false
    }

    fun joinWith(joinText: String): Normalizer = apply {
        joinWith = joinText
    }

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
        if (!caseSensitive) {
            normalizedText = normalizedText.lowercase()
        }
        /** Word tokenizer to split text into words. */
        val wordTokenizer = WordTokenizer(removePunctuation)

        normalizedText = wordTokenizer.tokenize(normalizedText.reader()).joinToString(joinWith) { word ->
            word.toString()
        }

        if (removeStopWords) {
            val stopWordTokenizer = StopWordTokenizer.new(requireNotNull(language) {
                "language is required for the stop words tokenizer"
            })
            normalizedText = stopWordTokenizer.tokenize(normalizedText.reader()).joinToString(joinWith) { word ->
                word.toString()
            }
        }

        if (stemming) {
            val stemmer = MultiLanguageStemmer.new(requireNotNull(language) { "language is required for the stemmer" })
            normalizedText = normalizedText.split(joinWith).joinToString(joinWith) { word ->
                stemmer.stem(word)
            }
        }

        return normalizedText
    }
}
