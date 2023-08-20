package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.FloatPoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.term(
    fieldName: String,
    value: Float,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    FloatPoint.newExactQuery(fieldName, value)
}

fun QueryBuilder.term(
    value: Float,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    FloatPoint.newExactQuery(field.name, value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: Float,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    FloatPoint.newExactQuery(field.name, value)
}

fun QueryBuilder.term(
    fieldName: String,
    value: FloatArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    FloatPoint.newSetQuery(fieldName, *value)
}

fun QueryBuilder.term(
    value: FloatArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    FloatPoint.newSetQuery(field.name, *value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: FloatArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    FloatPoint.newSetQuery(field.name, *value)
}
