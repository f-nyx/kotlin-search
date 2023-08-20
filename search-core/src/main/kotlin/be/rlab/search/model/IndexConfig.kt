package be.rlab.search.model

import be.rlab.nlp.model.Language
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.Similarity

data class IndexConfig(
    val supportedLanguages: List<Language>,
    val similarity: Similarity
) {
    companion object {
        fun new(
            supportedLanguages: List<Language>,
            similarity: Similarity
        ): IndexConfig = IndexConfig(
            supportedLanguages = supportedLanguages,
            similarity = similarity
        )

        fun default(): IndexConfig = IndexConfig(
            supportedLanguages = Language.entries,
            similarity = BM25Similarity()
        )
    }
}
