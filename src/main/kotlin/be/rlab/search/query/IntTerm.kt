package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.IntPoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.term(
    fieldName: String,
    value: Int,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    IntPoint.newExactQuery(fieldName, value)
}

fun QueryBuilder.term(
    value: Int,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    IntPoint.newExactQuery(field.name, value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: Int,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    IntPoint.newExactQuery(field.name, value)
}

fun QueryBuilder.term(
    fieldName: String,
    value: IntArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    IntPoint.newSetQuery(fieldName, *value)
}

fun QueryBuilder.term(
    value: IntArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    IntPoint.newSetQuery(field.name, *value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: IntArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    IntPoint.newSetQuery(field.name, *value)
}
