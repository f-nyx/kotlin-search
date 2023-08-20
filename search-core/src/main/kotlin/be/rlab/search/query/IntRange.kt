package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.IntPoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: Int,
    upperValue: Int,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    IntPoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: Int,
    upperValue: Int,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    IntPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: Int,
    upperValue: Int,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    IntPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: IntArray,
    upperValue: IntArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    IntPoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: IntArray,
    upperValue: IntArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    IntPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: IntArray,
    upperValue: IntArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    IntPoint.newRangeQuery(field.name, lowerValue, upperValue)
}
