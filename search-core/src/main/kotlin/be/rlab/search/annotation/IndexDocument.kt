package be.rlab.search.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IndexDocument(
    val namespace: String
)
