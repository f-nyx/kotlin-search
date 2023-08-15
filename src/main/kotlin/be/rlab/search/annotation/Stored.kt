package be.rlab.search.annotation

/** Mark this field to be stored in the index.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Deprecated("Use @IndexField(store = BoolValue.YES | BoolValue.NO) instead")
annotation class Stored(
    /** True to store the field value, false otherwise. */
    val value: Boolean = true
)
