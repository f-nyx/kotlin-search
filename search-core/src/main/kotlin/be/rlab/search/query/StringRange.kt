package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.TermRangeQuery
import kotlin.reflect.KProperty1

fun QueryBuilder.range(
    fieldName: String,
    lowerTerm: String,
    upperTerm: String,
    includeLower: Boolean = true,
    includeUpper: Boolean = true,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    TermRangeQuery.newStringRange(fieldName, lowerTerm, upperTerm, includeLower, includeUpper)
}

fun QueryBuilder.range(
    lowerTerm: String,
    upperTerm: String,
    includeLower: Boolean = true,
    includeUpper: Boolean = true,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    TermRangeQuery.newStringRange(field.name, lowerTerm, upperTerm, includeLower, includeUpper)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerTerm: String,
    upperTerm: String,
    includeLower: Boolean = true,
    includeUpper: Boolean = true,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    TermRangeQuery.newStringRange(field.name, lowerTerm, upperTerm, includeLower, includeUpper)
}
