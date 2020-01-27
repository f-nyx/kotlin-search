package be.rlab.search.model

import be.rlab.nlp.model.Language
import be.rlab.search.IndexManager
import org.apache.lucene.index.Term
import org.apache.lucene.search.*

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

    private var root: BooleanQuery.Builder = BooleanQuery.Builder()

    fun wildcard(
        fields: Map<String, String>,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST
    ): QueryBuilder {
        root = fields.entries.fold(root) { query, (fieldName, value) ->
            query.add(WildcardQuery(Term(fieldName, value)), occur)
        }
        return this
    }

    fun wildcard(
        fieldName: String,
        value: String,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST
    ): QueryBuilder {
        root = root.add(WildcardQuery(Term(fieldName, value)), occur)
        return this
    }

    fun term(
        fields: Map<String, String>,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST
    ): QueryBuilder {
        root = fields.entries.fold(root) { query, (fieldName, value) ->
            query.add(TermQuery(Term(fieldName, value)), occur)
        }
        return this
    }

    fun term(
        fieldName: String,
        value: String,
        occur: BooleanClause.Occur = BooleanClause.Occur.MUST
    ): QueryBuilder {
        root = root.add(TermQuery(Term(fieldName, value)), occur)
        return this
    }

    fun build(): Query {
        return root.build()
    }
}
