package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.TermQuery
import kotlin.reflect.KProperty1

fun QueryBuilder.term(
    fieldName: String,
    value: String,
    normalize: Boolean = true,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    TermQuery(Term(fieldName, normalizeIfRequired(value, normalize)))
}

fun QueryBuilder.term(
    value: String,
    normalize: Boolean = true,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    TermQuery(Term(field.name, normalizeIfRequired(value, normalize)))
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: String,
    normalize: Boolean = true,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    TermQuery(Term(field.name, normalizeIfRequired(value, normalize)))
}
