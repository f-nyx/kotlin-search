package be.rlab.search

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IndexField(
    val fieldName: String = "",
    val index: Boolean = true,
)
