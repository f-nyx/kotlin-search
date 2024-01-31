package be.rlab.search.model

import be.rlab.nlp.model.Language
import be.rlab.search.AnalyzerFactory
import be.rlab.search.DefaultAnalyzerFactory
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.Similarity

data class IndexConfig(
    val supportedLanguages: List<Language>,
    val similarity: Similarity,
    val analyzerFactory: AnalyzerFactory
) {
    companion object {
        fun new(
            supportedLanguages: List<Language>,
            similarity: Similarity,
            analyzerFactory: AnalyzerFactory
        ): IndexConfig = IndexConfig(
            supportedLanguages = supportedLanguages,
            similarity = similarity,
            analyzerFactory = analyzerFactory
        )

        fun default(): IndexConfig = IndexConfig(
            supportedLanguages = Language.entries,
            similarity = BM25Similarity(),
            analyzerFactory = DefaultAnalyzerFactory
        )
    }
}
