package be.rlab.search

import be.rlab.search.annotation.IndexDocument
import be.rlab.search.annotation.IndexField
import be.rlab.search.annotation.IndexFieldType
import be.rlab.search.model.BoolValue
import be.rlab.search.model.FieldType

@IndexDocument(namespace = IndexManagerTest.NAMESPACE)
data class Book(
    @IndexField val id: String,
    @IndexField(store = BoolValue.NO, index = BoolValue.YES) val title: String?,
    @IndexField val description: String,
    @IndexField(docValues = true) @IndexFieldType(FieldType.TEXT) val genre: String,
    @IndexField @IndexFieldType(FieldType.TEXT) val categories: List<String>,
    @IndexField val author: String,
    @IndexField(store = BoolValue.YES) val hash: Int,
    @IndexField(store = BoolValue.YES, index = BoolValue.YES) val rate: Float
)
