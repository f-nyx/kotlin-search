package be.rlab.search.model

/** Represents a three-state boolean to be used when nullable as a third-state
 * is not allowed, like in annotations.
 */
enum class BoolValue {
    YES,
    NO,
    DEFAULT;

    fun resolve(defaultValue: Boolean): Boolean {
        return if (this == DEFAULT) {
            defaultValue
        } else {
            this == YES
        }
    }
}
