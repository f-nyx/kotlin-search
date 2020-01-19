package be.rlab.search

import be.rlab.nlp.StopWordTokenizer
import be.rlab.nlp.model.Language
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.bg.BulgarianAnalyzer
import org.apache.lucene.analysis.bn.BengaliAnalyzer
import org.apache.lucene.analysis.br.BrazilianAnalyzer
import org.apache.lucene.analysis.ca.CatalanAnalyzer
import org.apache.lucene.analysis.ckb.SoraniAnalyzer
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.analysis.cz.CzechAnalyzer
import org.apache.lucene.analysis.da.DanishAnalyzer
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.analysis.el.GreekAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.es.SpanishAnalyzer
import org.apache.lucene.analysis.et.EstonianAnalyzer
import org.apache.lucene.analysis.eu.BasqueAnalyzer
import org.apache.lucene.analysis.fa.PersianAnalyzer
import org.apache.lucene.analysis.fi.FinnishAnalyzer
import org.apache.lucene.analysis.fr.FrenchAnalyzer
import org.apache.lucene.analysis.ga.IrishAnalyzer
import org.apache.lucene.analysis.gl.GalicianAnalyzer
import org.apache.lucene.analysis.hi.HindiAnalyzer
import org.apache.lucene.analysis.hu.HungarianAnalyzer
import org.apache.lucene.analysis.hy.ArmenianAnalyzer
import org.apache.lucene.analysis.id.IndonesianAnalyzer
import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.analysis.lt.LithuanianAnalyzer
import org.apache.lucene.analysis.lv.LatvianAnalyzer
import org.apache.lucene.analysis.no.NorwegianAnalyzer
import org.apache.lucene.analysis.pl.PolishAnalyzer
import org.apache.lucene.analysis.pt.PortugueseAnalyzer
import org.apache.lucene.analysis.ro.RomanianAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.sv.SwedishAnalyzer
import org.apache.lucene.analysis.th.ThaiAnalyzer
import org.apache.lucene.analysis.tr.TurkishAnalyzer
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object AnalyzerFactory {

    private val analyzerClassMap: Map<Language, KClass<out Analyzer>> = mapOf(
        Language.ARABIC to EnglishAnalyzer::class,
        Language.ARMENIAN to ArmenianAnalyzer::class,
        Language.BASQUE to BasqueAnalyzer::class,
        Language.BENGALI to BengaliAnalyzer::class,
        Language.BRAZILIAN to BrazilianAnalyzer::class,
        Language.BULGARIAN to BulgarianAnalyzer::class,
        Language.CATALAN to CatalanAnalyzer::class,
        Language.CHINESE to SmartChineseAnalyzer::class,
        Language.CZECH to CzechAnalyzer::class,
        Language.DANISH to DanishAnalyzer::class,
        Language.ENGLISH to EnglishAnalyzer::class,
        Language.ESTONIAN to EstonianAnalyzer::class,
        Language.FINNISH to FinnishAnalyzer::class,
        Language.FRENCH to FrenchAnalyzer::class,
        Language.GALICIAN to GalicianAnalyzer::class,
        Language.GERMAN to GermanAnalyzer::class,
        Language.GREEK to GreekAnalyzer::class,
        Language.HINDI to HindiAnalyzer::class,
        Language.HUNGARIAN to HungarianAnalyzer::class,
        Language.INDONESIAN to IndonesianAnalyzer::class,
        Language.IRISH to IrishAnalyzer::class,
        Language.ITALIAN to ItalianAnalyzer::class,
        Language.LATVIAN to LatvianAnalyzer::class,
        Language.LITHUANIAN to LithuanianAnalyzer::class,
        Language.NORWEGIAN to NorwegianAnalyzer::class,
        Language.PERSIAN to PersianAnalyzer::class,
        Language.POLISH to PolishAnalyzer::class,
        Language.PORTUGUESE to PortugueseAnalyzer::class,
        Language.ROMANIAN to RomanianAnalyzer::class,
        Language.RUSSIAN to RussianAnalyzer::class,
        Language.SORANI to SoraniAnalyzer::class,
        Language.SPANISH to SpanishAnalyzer::class,
        Language.SWEDISH to SwedishAnalyzer::class,
        Language.THAI to ThaiAnalyzer::class,
        Language.TURKISH to TurkishAnalyzer::class
    )

    fun newAnalyzer(language: Language): Analyzer {
        val klass = analyzerClassMap[language]
            ?: throw RuntimeException("language not supported: $language")

        val stopWords = StopWordTokenizer.stopWords(language)

        return if (stopWords.isEmpty()) {
            klass.createInstance()
        } else {
            val ctor = klass.constructors.find { ctor ->
                ctor.parameters.size == 1 && ctor.parameters.first().type.classifier == CharArraySet::class
            } ?: throw RuntimeException("constructor with stop words not found")
            ctor.call(CharArraySet(stopWords, false))
        }
    }
}
