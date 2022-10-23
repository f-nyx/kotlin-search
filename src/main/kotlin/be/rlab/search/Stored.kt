package be.rlab.search

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Stored(
    val value: Boolean
)
