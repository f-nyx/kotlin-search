package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.DoublePoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: Double,
    upperValue: Double,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    DoublePoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: Double,
    upperValue: Double,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    DoublePoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: Double,
    upperValue: Double,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    DoublePoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: DoubleArray,
    upperValue: DoubleArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    DoublePoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: DoubleArray,
    upperValue: DoubleArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    DoublePoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: DoubleArray,
    upperValue: DoubleArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    DoublePoint.newRangeQuery(field.name, lowerValue, upperValue)
}
