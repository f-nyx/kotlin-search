package be.rlab.support

import be.rlab.nlp.Normalizer
import be.rlab.nlp.model.Language

object SearchTestUtils {
    fun firstWord(text: String, language: Language = Language.ENGLISH): String {
        return Normalizer(text, language, stemming = false, removeStopWords = true)
            .normalize()
            .split(" ")
            .first { word -> word.length > 3 }
    }

    fun lastWord(text: String, language: Language = Language.ENGLISH): String {
        return Normalizer(text, language, stemming = false, removeStopWords = true)
            .normalize()
            .split(" ")
            .reversed()
            .first { word -> word.length > 3 }
    }
}
