package be.rlab.search.model

import be.rlab.nlp.model.Language
import be.rlab.search.IndexManager
import org.apache.lucene.document.DoublePoint
import org.apache.lucene.document.FloatPoint
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.util.automaton.RegExp

/** Builder to create Lucene [Query]s.
 */
class QueryBuilder private constructor (
    val language: Language
) {

    companion object {
        fun query(
            namespace: String,
            language: Language
        ): QueryBuilder {
            val builder = QueryBuilder(language)

            return builder.apply {
                term(IndexManager.NAMESPACE_FIELD, namespace)
            }
        }
    }

    class QueryModifiers(
        internal var boost: Float = -1.0F
    ) {
        fun boost(score: Float) {
            boost = score
        }
    }

    private var root: BooleanQuery.Builder = BooleanQuery.Builder()

    fun wildcard(
        fieldName: String,
        value: String,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            WildcardQuery(Term(fieldName, value)),
            callback
        ), occur)
        return this
    }

    fun fuzzy(
        fieldName: String,
        value: String,
        maxEdits: Int = FuzzyQuery.defaultMaxEdits,
        prefixLength: Int = FuzzyQuery.defaultPrefixLength,
        maxExpansions: Int = FuzzyQuery.defaultMaxExpansions,
        transpositions: Boolean = FuzzyQuery.defaultTranspositions,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            FuzzyQuery(Term(fieldName, value), maxEdits, prefixLength, maxExpansions, transpositions),
            callback
        ), occur)
        return this
    }

    fun regex(
        fieldName: String,
        value: Regex,
        flags: Int = RegExp.ALL,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            RegexpQuery(Term(fieldName, value.pattern), flags),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: String,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            TermQuery(Term(fieldName, value)),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: Int,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            IntPoint.newExactQuery(fieldName, value),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: IntArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            IntPoint.newSetQuery(fieldName, *value),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: Long,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            LongPoint.newExactQuery(fieldName, value),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: LongArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            LongPoint.newSetQuery(fieldName, *value),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: Float,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            FloatPoint.newExactQuery(fieldName, value),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: FloatArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            FloatPoint.newSetQuery(fieldName, *value),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: Double,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            DoublePoint.newExactQuery(fieldName, value),
            callback
        ), occur)
        return this
    }

    fun term(
        fieldName: String,
        value: DoubleArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            DoublePoint.newSetQuery(fieldName, *value),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: Int,
        upperValue: Int,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            IntPoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: IntArray,
        upperValue: IntArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            IntPoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: Long,
        upperValue: Long,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            LongPoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: LongArray,
        upperValue: LongArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            LongPoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: Float,
        upperValue: Float,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            FloatPoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: FloatArray,
        upperValue: FloatArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            FloatPoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: Double,
        upperValue: Double,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            DoublePoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun range(
        fieldName: String,
        lowerValue: DoubleArray,
        upperValue: DoubleArray,
        occur: BooleanClause.Occur = BooleanClause.Occur.SHOULD,
        callback: QueryModifiers.() -> Unit = {}
    ): QueryBuilder {
        root = root.add(withModifiers(
            DoublePoint.newRangeQuery(fieldName, lowerValue, upperValue),
            callback
        ), occur)
        return this
    }

    fun build(): Query {
        return root.build()
    }

    private fun withModifiers(
        query: Query,
        callback: QueryModifiers.() -> Unit
    ): Query {
        val modifiers = QueryModifiers()
        callback(modifiers)

        return if (modifiers.boost >= 0) {
            BoostQuery(query, modifiers.boost)
        } else {
            query
        }
    }
}
