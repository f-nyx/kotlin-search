package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.RegexpQuery
import org.apache.lucene.util.automaton.RegExp
import kotlin.reflect.KProperty1

fun QueryBuilder.regex(
    fieldName: String,
    value: Regex,
    flags: Int = RegExp.ALL,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    RegexpQuery(Term(fieldName, value.pattern), flags)
}

fun QueryBuilder.regex(
    value: Regex,
    flags: Int = RegExp.ALL,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    RegexpQuery(Term(field.name, value.pattern), flags)
}

fun<T : Any> QueryBuilder.regex(
    property: KProperty1<T, *>,
    value: Regex,
    flags: Int = RegExp.ALL,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    RegexpQuery(Term(field.name, value.pattern), flags)
}
