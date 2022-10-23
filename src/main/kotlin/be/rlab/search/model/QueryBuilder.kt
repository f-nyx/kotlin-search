package be.rlab.search.model

import be.rlab.nlp.Normalizer
import be.rlab.nlp.model.Language
import be.rlab.search.IndexManager.Companion.NAMESPACE_FIELD
import be.rlab.search.IndexManager.Companion.PRIVATE_FIELD_PREFIX
import be.rlab.search.query.term
import org.apache.lucene.search.*
import kotlin.reflect.KProperty1

/** Builder to create Lucene [Query]s.
 */
class QueryBuilder private constructor (
    val language: Language,
    val fields: List<FieldMetadata>
) {

    companion object {
        fun query(
            namespace: String,
            language: Language
        ): QueryBuilder {
            val builder = QueryBuilder(language, fields = emptyList())

            return builder.apply {
                term(NAMESPACE_FIELD, namespace, normalize = false)
            }
        }

        fun query(
            metadata: DocumentMetadata<*>,
            language: Language
        ): QueryBuilder {
            val builder = QueryBuilder(language, fields = metadata.fields)

            return builder.apply {
                term("$PRIVATE_FIELD_PREFIX$NAMESPACE_FIELD", metadata.namespace, normalize = false)
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

    fun build(): Query {
        return root.build()
    }

    fun findByField(
        occur: BooleanClause.Occur,
        callback: QueryModifiers.() -> Unit,
        builder: () -> Query
    ): QueryBuilder = apply {
        root.add(withModifiers(
            builder(),
            callback
        ), occur)
    }

    fun<T : Any> findByProperty(
        property: KProperty1<T, *>,
        occur: BooleanClause.Occur,
        callback: QueryModifiers.() -> Unit,
        builder: (FieldMetadata) -> Query
    ): QueryBuilder = apply {
        require(fields.isNotEmpty()) { "QueryBuilder does not support search by multiple fields" }

        val field = fields.find { field -> field.propertyName == property.name }
            ?: throw RuntimeException("property '${property.name}' not annotated with @IndexField")

        root.add(withModifiers(
            builder(field),
            callback
        ), occur)
    }

    fun findByAllFields(
        occur: BooleanClause.Occur,
        callback: QueryModifiers.() -> Unit,
        builder: (FieldMetadata) -> Query
    ): QueryBuilder = apply {
        require(fields.isNotEmpty()) { "QueryBuilder does not support search by multiple fields" }
        val queries = fields.map(builder)
        val child: BooleanQuery.Builder = queries.fold(BooleanQuery.Builder()) { aggregate, query ->
            aggregate.add(withModifiers(query, callback), BooleanClause.Occur.SHOULD)
        }
        root.add(child.build(), occur)
    }

    fun custom(callback: (BooleanQuery.Builder) -> Unit): QueryBuilder = apply {
        callback(root)
    }

    fun normalizeIfRequired(value: String, normalize: Boolean = false): String {
        return if (normalize) {
            Normalizer(value, language).normalize()
        } else {
            value
        }
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
