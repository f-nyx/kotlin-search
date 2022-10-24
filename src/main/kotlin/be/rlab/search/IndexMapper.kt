package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.model.*

class IndexMapper(
    val indexManager: IndexManager
) {
    /** Analyzes and indexes a document reading the configuration from annotations.
     * @param source Document to index. Must be annotated with the proper annotations.
     */
    @Suppress("UNCHECKED_CAST")
    fun<T : Any> index(source: T) {
        val reader = DocumentSchema.fromClass(source::class) as DocumentSchema<T>
        reader.buildDocuments(source).forEach { doc ->
            indexManager.index(doc)
        }
    }

    /** Search for documents in a specific language.
     *
     * The query builder provides a flexible interface to build Lucene queries.
     *
     * The cursor and the limit allows to paginate the search results. If you provide a cursor returned
     * in a previous [SearchResult], this method resumes the search from there.
     *
     * @param namespace Documents namespace.
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
        val schema: DocumentSchema<T> = DocumentSchema.fromClass(T::class)
        val query = QueryBuilder.forSchema(schema, language).apply(builder)
        val result = indexManager.search(query, cursor, limit)

        return TypedSearchResult(
            docs = result.mapAs(T::class),
            total = result.total,
            next = result.next
        )
    }
}
