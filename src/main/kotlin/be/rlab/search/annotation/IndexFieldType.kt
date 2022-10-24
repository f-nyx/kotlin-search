package be.rlab.search.annotation

import be.rlab.search.model.FieldType

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IndexFieldType(
    val type: FieldType
)
