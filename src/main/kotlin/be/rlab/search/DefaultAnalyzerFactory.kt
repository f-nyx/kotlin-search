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
import org.apache.lucene.analysis.ne.NepaliAnalyzer
import org.apache.lucene.analysis.nl.DutchAnalyzer
import org.apache.lucene.analysis.no.NorwegianAnalyzer
import org.apache.lucene.analysis.pl.PolishAnalyzer
import org.apache.lucene.analysis.pt.PortugueseAnalyzer
import org.apache.lucene.analysis.ro.RomanianAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.sr.SerbianAnalyzer
import org.apache.lucene.analysis.sv.SwedishAnalyzer
import org.apache.lucene.analysis.th.ThaiAnalyzer
import org.apache.lucene.analysis.tr.TurkishAnalyzer
import kotlin.reflect.full.createInstance

object DefaultAnalyzerFactory: AnalyzerFactory {
    override fun newAnalyzer(language: Language): Analyzer {
        val klass = when(language) {
            Language.ARABIC -> EnglishAnalyzer::class
            Language.ARMENIAN -> ArmenianAnalyzer::class
            Language.BASQUE -> BasqueAnalyzer::class
            Language.BENGALI -> BengaliAnalyzer::class
            Language.BRAZILIAN -> BrazilianAnalyzer::class
            Language.BULGARIAN -> BulgarianAnalyzer::class
            Language.CATALAN -> CatalanAnalyzer::class
            Language.CHINESE -> SmartChineseAnalyzer::class
            Language.CZECH -> CzechAnalyzer::class
            Language.DANISH -> DanishAnalyzer::class
            Language.DUTCH -> DutchAnalyzer::class
            Language.ENGLISH -> EnglishAnalyzer::class
            Language.ESTONIAN -> EstonianAnalyzer::class
            Language.FINNISH -> FinnishAnalyzer::class
            Language.FRENCH -> FrenchAnalyzer::class
            Language.GALICIAN -> GalicianAnalyzer::class
            Language.GERMAN -> GermanAnalyzer::class
            Language.GREEK -> GreekAnalyzer::class
            Language.HINDI -> HindiAnalyzer::class
            Language.HUNGARIAN -> HungarianAnalyzer::class
            Language.INDONESIAN -> IndonesianAnalyzer::class
            Language.IRISH -> IrishAnalyzer::class
            Language.ITALIAN -> ItalianAnalyzer::class
            Language.LATVIAN -> LatvianAnalyzer::class
            Language.LITHUANIAN -> LithuanianAnalyzer::class
            Language.NEPALI -> NepaliAnalyzer::class
            Language.NORWEGIAN -> NorwegianAnalyzer::class
            Language.PERSIAN -> PersianAnalyzer::class
            Language.POLISH -> PolishAnalyzer::class
            Language.PORTUGUESE -> PortugueseAnalyzer::class
            Language.ROMANIAN -> RomanianAnalyzer::class
            Language.RUSSIAN -> RussianAnalyzer::class
            Language.SERBIAN -> SerbianAnalyzer::class
            Language.SORANI -> SoraniAnalyzer::class
            Language.SPANISH -> SpanishAnalyzer::class
            Language.SWEDISH -> SwedishAnalyzer::class
            Language.THAI -> ThaiAnalyzer::class
            Language.TURKISH -> TurkishAnalyzer::class
        }

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