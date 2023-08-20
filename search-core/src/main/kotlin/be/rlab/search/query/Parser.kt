package be.rlab.search.query

import be.rlab.search.AnalyzerFactory
import be.rlab.search.model.QueryBuilder
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause

fun QueryBuilder.parse(
    defaultFieldName: String,
    query: String,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    val parser = QueryParser(defaultFieldName, AnalyzerFactory.newAnalyzer(language))
    parser.parse(query)
}
