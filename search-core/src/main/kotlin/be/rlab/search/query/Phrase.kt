package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.PhraseQuery
import kotlin.reflect.KProperty1

fun QueryBuilder.phrase(
    fieldName: String,
    vararg values: String,
    normalize: Boolean = true,
    maxEditDistance: Int = 0,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    val terms = values.map { value -> normalizeIfRequired(value, normalize) }.toTypedArray()
    PhraseQuery(maxEditDistance, fieldName, *terms)
}

fun QueryBuilder.phrase(
    vararg values: String,
    normalize: Boolean = true,
    maxEditDistance: Int = 0,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    val terms = values.map { value -> normalizeIfRequired(value, normalize) }.toTypedArray()
    PhraseQuery(maxEditDistance, field.name, *terms)
}

fun<T : Any> QueryBuilder.phrase(
    property: KProperty1<T, *>,
    vararg values: String,
    normalize: Boolean = true,
    maxEditDistance: Int = 0,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    val terms = values.map { value -> normalizeIfRequired(value, normalize) }.toTypedArray()
    PhraseQuery(maxEditDistance, field.name, *terms)
}
