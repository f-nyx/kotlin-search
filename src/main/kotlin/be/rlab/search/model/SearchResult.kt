package be.rlab.search.model

/** Represents a set of paginated search results.
 */
data class SearchResult(
    /** Search results. */
    val docs: List<Document>,
    /** Total number of documents in the search. */
    val total: Long,
    /** Cursor to retrieve the next page. */
    val next: Cursor?
) {
    companion object {
        fun new(
            results: List<Document>,
            total: Long,
            next: Cursor? = null
        ): SearchResult =
            SearchResult(
                docs = results,
                total = total,
                next = next
            )
    }
}
