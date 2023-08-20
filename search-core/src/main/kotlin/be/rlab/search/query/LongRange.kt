package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.LongPoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: Long,
    upperValue: Long,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    LongPoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: Long,
    upperValue: Long,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    LongPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: Long,
    upperValue: Long,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    LongPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun QueryBuilder.range(
    fieldName: String,
    lowerValue: LongArray,
    upperValue: LongArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    LongPoint.newRangeQuery(fieldName, lowerValue, upperValue)
}

fun QueryBuilder.range(
    lowerValue: LongArray,
    upperValue: LongArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    LongPoint.newRangeQuery(field.name, lowerValue, upperValue)
}

fun<T : Any> QueryBuilder.range(
    property: KProperty1<T, *>,
    lowerValue: LongArray,
    upperValue: LongArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    LongPoint.newRangeQuery(field.name, lowerValue, upperValue)
}
