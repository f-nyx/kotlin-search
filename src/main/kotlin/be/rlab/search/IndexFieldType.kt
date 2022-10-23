package be.rlab.search

import be.rlab.search.model.FieldType

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IndexFieldType(
    val type: FieldType
)
