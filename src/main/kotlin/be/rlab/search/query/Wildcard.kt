package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.WildcardQuery
import kotlin.reflect.KProperty1

fun QueryBuilder.wildcard(
    fieldName: String,
    value: String,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    WildcardQuery(Term(fieldName, value))
}

fun QueryBuilder.wildcard(
    value: String,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    WildcardQuery(Term(field.name, value))
}

fun<T : Any> QueryBuilder.wildcard(
    property: KProperty1<T, *>,
    value: String,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    WildcardQuery(Term(field.name, value))
}
