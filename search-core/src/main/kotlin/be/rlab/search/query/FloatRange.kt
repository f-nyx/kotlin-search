package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.FloatPoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: Float,
    upperValue: Float,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    FloatPoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: Float,
    upperValue: Float,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    FloatPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: Float,
    upperValue: Float,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    FloatPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: FloatArray,
    upperValue: FloatArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    FloatPoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: FloatArray,
    upperValue: FloatArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    FloatPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: FloatArray,
    upperValue: FloatArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    FloatPoint.newRangeQuery(field.name, lowerValue, upperValue)
}
