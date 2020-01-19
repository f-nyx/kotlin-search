package be.rlab.search.model

/** Index supported field types.
 */
enum class FieldType {
    /** A String field that is stored but not tokenized.
     */
    STRING,
    /** A field that is stored and tokenized.
     */
    TEXT
}
