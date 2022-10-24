package be.rlab.search.annotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Stored(
    val value: Boolean
)
