package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.model.*
import be.rlab.search.schema.DocumentSchemaBuilder
import be.rlab.search.mapper.FieldTypeMapper
import be.rlab.search.mapper.ListTypeMapper
import be.rlab.search.mapper.SimpleTypeMapper
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class IndexMapper(
    val indexManager: IndexManager,
    fieldTypeMappers: List<FieldTypeMapper> = emptyList()
) {
    val fieldTypeMappers: List<FieldTypeMapper> = fieldTypeMappers + listOf(
        SimpleTypeMapper(),
        ListTypeMapper()
    )

    /** Analyzes and indexes a document reading the configuration from annotations.
     * @param source Document to index. Must be annotated with the proper annotations.
     */
    fun<T : Any> index(source: T) {
        val schema = DocumentSchemaBuilder.buildFromClass(source::class, fieldTypeMappers) as DocumentSchema
        DocumentBuilder.buildFromObject(schema, source, LuceneIndex.CURRENT_VERSION).forEach { doc ->
            indexManager.index(doc)
        }
    }

    /** Search for documents in a specific language.
     *
     * The query builder provides a flexible interface to build Lucene queries.
     *
     * The cursor and the limit allow to paginate the search results. If you provide a cursor returned
     * in a previous [SearchResult], this method resumes the search from there.
     *
     * @param language Language of the index to search.
     * @param cursor Cursor to resume a paginated search.
     * @param limit Max number of results to retrieve.
     * @param builder Query builder.
     */
    inline fun<reified T : Any> search(
        language: Language,
        cursor: Cursor = Cursor.first(),
        limit: Int = IndexManager.DEFAULT_LIMIT,
        builder: QueryBuilder.() -> Unit
    ): TypedSearchResult<T> {
        val schema: DocumentSchema = DocumentSchemaBuilder.buildFromClass(T::class, fieldTypeMappers)
        val query = QueryBuilder.forSchema(schema, language).apply(builder)
        val result = indexManager.search(query, cursor, limit)

        return TypedSearchResult(
            docs = result.docs.map { source -> convert(source, T::class) },
            total = result.total,
            next = result.next
        )
    }

    fun<T : Any> convert(source: Document, targetType: KClass<T>): T {
        val docSchema = DocumentSchemaBuilder.buildFromClass(targetType, fieldTypeMappers)
        val constructor = targetType.primaryConstructor ?: throw RuntimeException("no primary constructor found")
        val values: List<Any?> = constructor.parameters.map { param ->
            val mapper = fieldTypeMappers.firstOrNull { mapper -> mapper.supports(param.type) }
                ?: throw RuntimeException("no mapper found for type: ${param.type}")
            val value: Any? = mapper.mapValue(param.type, param.name!!, docSchema, source)
            require(param.type.isMarkedNullable || value != null) { "field value cannot be null: name=${param.name}" }
            value
        }
        return constructor.call(*values.toTypedArray())
    }

    inline fun<reified T : Any> convert(source: Document): T {
        return convert(source, T::class)
    }
}
