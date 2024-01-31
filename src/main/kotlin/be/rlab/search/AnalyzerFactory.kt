package be.rlab.search

import be.rlab.nlp.model.Language
import org.apache.lucene.analysis.Analyzer

interface AnalyzerFactory {
    fun newAnalyzer(language: Language): Analyzer
}

