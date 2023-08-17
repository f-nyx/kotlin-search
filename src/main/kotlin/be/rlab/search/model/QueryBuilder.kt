package be.rlab.search.model

import be.rlab.nlp.Normalizer
import be.rlab.nlp.model.Language
import be.rlab.search.LuceneFieldUtils.privateField
import be.rlab.search.LuceneIndex.Companion.NAMESPACE_FIELD
import be.rlab.search.query.term
import org.apache.lucene.search.*
import kotlin.reflect.KProperty1

/** Builder to create Lucene [Query]s.
 */
class QueryBuilder private constructor (
    val language: Language,
    val fields: List<FieldSchema>
) {

    companion object {
        fun query(
            namespace: String,
            language: Language
        ): QueryBuilder {
            val builder = QueryBuilder(language, fields = emptyList())

            return builder.apply {
                term(privateField(NAMESPACE_FIELD), namespace, normalize = false)
            }
        }

        fun forSchema(
            schema: DocumentSchema,
            language: Language
        ): QueryBuilder {
            val builder = QueryBuilder(language, fields = schema.fields)

            return builder.apply {
                term(privateField(NAMESPACE_FIELD), schema.namespace, normalize = false)
            }
        }
    }

    class QueryModifiers(
        internal var boost: Float = -1.0F,
        internal val searchBy: MutableList<String> = mutableListOf()
    ) {
        fun boost(score: Float) {
            boost = score
        }

        fun by(vararg fields: String) {
            searchBy.addAll(fields.toList())
        }
    }

    private var root: BooleanQuery.Builder = BooleanQuery.Builder()
    private val sortFields: MutableList<SortField> = mutableListOf()

    fun build(): Query {
        return root.build()
    }

    fun sort(): Sort? {
        return if (sortFields.isNotEmpty()) {
            Sort(*sortFields.toTypedArray())
        } else {
            null
        }
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
        builder: (FieldSchema) -> Query
    ): QueryBuilder = apply {
        require(fields.isNotEmpty()) { "QueryBuilder does not support search by multiple fields" }

        val field = requireNotNull(getFieldSchema(property.name)) {
            "property '${property.name}' not annotated with @IndexField"
        }

        root.add(withModifiers(
            builder(field),
            callback
        ), occur)
    }

    fun findByAllFields(
        occur: BooleanClause.Occur,
        callback: QueryModifiers.() -> Unit,
        builder: (FieldSchema) -> Query
    ): QueryBuilder = apply {
        require(fields.isNotEmpty()) { "QueryBuilder does not support search by multiple fields" }
        val modifiers = QueryModifiers().apply(callback)
        val selectedFields = if (modifiers.searchBy.isNotEmpty()) {
            fields.filter { field -> modifiers.searchBy.contains(field.name) }
        } else {
            fields
        }
        val queries = selectedFields.map(builder)
        val child: BooleanQuery.Builder = queries.fold(BooleanQuery.Builder()) { aggregate, query ->
            aggregate.add(withModifiers(query, modifiers), BooleanClause.Occur.SHOULD)
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

    /** Returns a field schema by name or by property name.
     * @param name Field name or property name.
     * @return the required schema, if exists.
     */
    fun getFieldSchema(name: String): FieldSchema? {
        return fields.find { field -> field.name == name || field.propertyName == name }
    }

    /** Adds a sorting criteria to the list of existing criteria.
     * @param sortField Sorting criteria.
     */
    fun addSortField(sortField: SortField): QueryBuilder = apply {
        sortFields += sortField
    }

    private fun withModifiers(
        query: Query,
        callback: QueryModifiers.() -> Unit
    ): Query =
        withModifiers(query, QueryModifiers().apply(callback))

    private fun withModifiers(
        query: Query,
        modifiers: QueryModifiers
    ): Query {
        return if (modifiers.boost >= 0) {
            BoostQuery(query, modifiers.boost)
        } else {
            query
        }
    }
}
