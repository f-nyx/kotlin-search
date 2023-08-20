package be.rlab.search.model

data class Cursor(
    val docId: Int,
    val score: Float,
    val shardIndex: Int
) {
    companion object {
        fun first(): Cursor =
            Cursor(
                docId = -1,
                score = 0.toFloat(),
                shardIndex = 0
            )
    }

    fun isFirst(): Boolean =
        docId == -1
}
