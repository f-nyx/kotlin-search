package be.rlab.search.annotation

/** Mark this field to be indexed.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Deprecated("Use @IndexField(index = BoolValue.YES | BoolValue.NO) instead")
annotation class Indexed(
    /** True to index the field, false otherwise. */
    val value: Boolean = true
)
