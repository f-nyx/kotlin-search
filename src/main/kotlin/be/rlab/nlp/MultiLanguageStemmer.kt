package be.rlab.nlp

import be.rlab.nlp.model.Language
import org.tartarus.snowball.SnowballStemmer
import org.tartarus.snowball.ext.*

/** Tartarus snowball stemmer wrapper.
 *
 * It supports all languages defined in the [Language] enumeration.
 */
class MultiLanguageStemmer(
    private val stemmer: SnowballStemmer
) {

    companion object {
        /** Creates a new stemmer for the specified language.
         * @param language Stemmer language.
         * @return the required stemmer.
         */
        fun new(language: Language): MultiLanguageStemmer {
            return MultiLanguageStemmer(when (language) {
                Language.ARABIC -> ArabicStemmer()
                Language.ARMENIAN -> ArmenianStemmer()
                Language.BASQUE -> BasqueStemmer()
                Language.CATALAN -> CatalanStemmer()
                Language.DANISH -> DanishStemmer()
                Language.DUTCH -> DutchStemmer()
                Language.ENGLISH -> EnglishStemmer()
                Language.ESTONIAN -> EstonianStemmer()
                Language.FINNISH -> FinnishStemmer()
                Language.FRENCH -> FrenchStemmer()
                Language.GERMAN -> German2Stemmer()
                Language.GREEK -> GreekStemmer()
                Language.HINDI -> HindiStemmer()
                Language.HUNGARIAN -> HungarianStemmer()
                Language.INDONESIAN -> IndonesianStemmer()
                Language.IRISH -> IrishStemmer()
                Language.ITALIAN -> ItalianStemmer()
                Language.LITHUANIAN -> LithuanianStemmer()
                Language.NEPALI -> NepaliStemmer()
                Language.NORWEGIAN -> NorwegianStemmer()
                Language.PORTUGUESE -> PortugueseStemmer()
                Language.ROMANIAN -> RomanianStemmer()
                Language.RUSSIAN -> RussianStemmer()
                Language.SERBIAN -> SerbianStemmer()
                Language.SPANISH -> SpanishStemmer()
                Language.SWEDISH -> SwedishStemmer()
                Language.TURKISH -> TurkishStemmer()
                else -> throw RuntimeException("stemmer for language $language not supported")
            })
        }
    }

    /** Applies the stemmer to a text.
     * @param text Text to apply the stemmer.
     * @return the stemmed text.
     */
    fun stem(text: String): String {
        stemmer.current = text
        stemmer.stem()
        return stemmer.current
    }
}
