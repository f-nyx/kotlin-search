package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.DoublePoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.term(
    fieldName: String,
    value: Double,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    DoublePoint.newExactQuery(fieldName, value)
}

fun QueryBuilder.term(
    value: Double,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    DoublePoint.newExactQuery(field.name, value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: Double,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    DoublePoint.newExactQuery(field.name, value)
}

fun QueryBuilder.term(
    fieldName: String,
    value: DoubleArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    DoublePoint.newSetQuery(fieldName, *value)
}

fun QueryBuilder.term(
    value: DoubleArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    DoublePoint.newSetQuery(field.name, *value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: DoubleArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    DoublePoint.newSetQuery(field.name, *value)
}
