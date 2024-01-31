package be.rlab.search.query

import be.rlab.search.AnalyzerFactory
import be.rlab.search.DefaultAnalyzerFactory
import be.rlab.search.model.QueryBuilder
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause

fun QueryBuilder.parse(
    defaultFieldName: String,
    query: String,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    analyzerFactory: AnalyzerFactory = DefaultAnalyzerFactory,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    val parser = QueryParser(defaultFieldName, analyzerFactory.newAnalyzer(language))
    parser.parse(query)
}
