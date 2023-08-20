package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.document.LongPoint
import org.apache.lucene.search.BooleanClause
import kotlin.reflect.KProperty1

fun QueryBuilder.term(
    fieldName: String,
    value: Long,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    LongPoint.newExactQuery(fieldName, value)
}

fun QueryBuilder.term(
    value: Long,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    LongPoint.newExactQuery(field.name, value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: Long,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    LongPoint.newExactQuery(field.name, value)
}

fun QueryBuilder.term(
    fieldName: String,
    value: LongArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    LongPoint.newSetQuery(fieldName, *value)
}

fun QueryBuilder.term(
    value: LongArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    LongPoint.newSetQuery(field.name, *value)
}

fun<T : Any> QueryBuilder.term(
    property: KProperty1<T, *>,
    value: LongArray,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    LongPoint.newSetQuery(field.name, *value)
}
