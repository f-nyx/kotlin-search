package be.rlab.search.query

import be.rlab.search.model.QueryBuilder
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.FuzzyQuery
import kotlin.reflect.KProperty1

fun QueryBuilder.fuzzy(
    fieldName: String,
    value: String,
    normalize: Boolean = true,
    maxEdits: Int = FuzzyQuery.defaultMaxEdits,
    prefixLength: Int = FuzzyQuery.defaultPrefixLength,
    maxExpansions: Int = FuzzyQuery.defaultMaxExpansions,
    transpositions: Boolean = FuzzyQuery.defaultTranspositions,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByField(occur, callback) {
    val term = Term(fieldName, normalizeIfRequired(value, normalize))
    FuzzyQuery(term, maxEdits, prefixLength, maxExpansions, transpositions)
}

fun QueryBuilder.fuzzy(
    value: String,
    normalize: Boolean = true,
    maxEdits: Int = FuzzyQuery.defaultMaxEdits,
    prefixLength: Int = FuzzyQuery.defaultPrefixLength,
    maxExpansions: Int = FuzzyQuery.defaultMaxExpansions,
    transpositions: Boolean = FuzzyQuery.defaultTranspositions,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByAllFields(occur, callback) { field ->
    val term = Term(field.name, normalizeIfRequired(value, normalize))
    FuzzyQuery(term, maxEdits, prefixLength, maxExpansions, transpositions)
}

fun<T : Any> QueryBuilder.fuzzy(
    property: KProperty1<T, *>,
    value: String,
    normalize: Boolean = true,
    maxEdits: Int = FuzzyQuery.defaultMaxEdits,
    prefixLength: Int = FuzzyQuery.defaultPrefixLength,
    maxExpansions: Int = FuzzyQuery.defaultMaxExpansions,
    transpositions: Boolean = FuzzyQuery.defaultTranspositions,
    occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
    callback: QueryBuilder.QueryModifiers.() -> Unit = {}
): QueryBuilder = findByProperty(property, occur, callback) { field ->
    val term = Term(field.name, normalizeIfRequired(value, normalize))
    FuzzyQuery(term, maxEdits, prefixLength, maxExpansions, transpositions)
}
