package be.rlab.search.annotation

import be.rlab.search.model.BoolValue

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IndexField(
    @Deprecated("use name instead")
    val fieldName: String = "",
    val name: String = "",
    val index: BoolValue = BoolValue.DEFAULT,
    val docValues: Boolean = false,
    val store: BoolValue = BoolValue.DEFAULT,
)
