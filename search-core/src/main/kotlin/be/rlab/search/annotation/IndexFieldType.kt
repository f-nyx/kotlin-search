package be.rlab.search.annotation

import be.rlab.search.model.FieldType

/** Overrides the type inference and sets the field type in the index.
 * If this type is not compatible with the Kotlin type, it will throw an error at index time.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IndexFieldType(
    val type: FieldType
)
