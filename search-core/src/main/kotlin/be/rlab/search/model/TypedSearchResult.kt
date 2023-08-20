package be.rlab.search.model

/** Contains the paginated search results mapped to the target type.
 */
data class TypedSearchResult<T : Any>(
    /** Search results. */
    val docs: List<T>,
    /** Total number of documents in the search. */
    val total: Long,
    /** Cursor to retrieve the next page. */
    val next: Cursor?
) {
    companion object {
        fun<T : Any> new(
            results: List<T>,
            total: Long,
            next: Cursor? = null
        ): TypedSearchResult<T> =
            TypedSearchResult(
                docs = results,
                total = total,
                next = next
            )
    }
}
