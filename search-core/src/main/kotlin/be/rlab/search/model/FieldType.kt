package be.rlab.search.model

/** Index supported field types.
 */
enum class FieldType(
    val stored: Boolean,
    val indexed: Boolean
) {
    /** A String field that is stored but not tokenized.
     */
    STRING(stored = true, indexed = false),
    /** A field that is stored and indexed.
     */
    TEXT(stored = true, indexed = true),
    /** Integer value for exact/range queries. By default numeric types are not stored. */
    INT(stored = false, indexed = true),
    /** Long value for exact/range queries. By default numeric types are not stored. */
    LONG(stored = false, indexed = true),
    /** Float value for exact/range queries. By default numeric types are not stored. */
    FLOAT(stored = false, indexed = true),
    /** Double value for exact/range queries. By default numeric types are not stored. */
    DOUBLE(stored = false, indexed = true)
}
