package be.rlab.search.model

/** Index supported field types.
 */
enum class FieldType {
    /** A String field that is stored but not tokenized.
     */
    STRING,
    /** A field that is stored and tokenized.
     */
    TEXT,
    /** Integer value for exact/range queries. */
    INT,
    /** Long value for exact/range queries. */
    LONG,
    /** Float value for exact/range queries. */
    FLOAT,
    /** Double value for exact/range queries. */
    DOUBLE
}
