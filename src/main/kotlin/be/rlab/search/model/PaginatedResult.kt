package be.rlab.search.model

/** Represents a set of paginated search results.
 *
 * @param results Search results.
 * @param total Total number of documents in the search.
 * @param next Cursor to retrieve the next page.
 * @param previous Cursor to retrieve the previous page.
 */
data class PaginatedResult(
    val results: List<Document>,
    val total: Long,
    val next: Cursor?,
    val previous: Cursor?
) {
    companion object {
        fun new(
            results: List<Document>,
            total: Long,
            next: Cursor? = null,
            previous: Cursor? = null
        ): PaginatedResult =
            PaginatedResult(
                results = results,
                total = total,
                next = next,
                previous = previous
            )
    }

    /** Indicates whether this paginated search has additional results to retrieve.
     * @return true if there are additional results.
     */
    fun hasMoreItems(): Boolean =
        next != null
}