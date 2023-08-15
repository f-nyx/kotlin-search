package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.annotation.*
import be.rlab.search.model.BoolValue
import be.rlab.search.model.FieldType

@IndexDocument(namespace = IndexManagerTest.NAMESPACE, languages = [Language.SPANISH, Language.ENGLISH])
data class Book(
    @IndexField val id: String,
    @IndexField(store = BoolValue.YES, index = BoolValue.YES) val hash: Int,
    @IndexField(store = BoolValue.NO) val title: String?,
    @IndexField val description: String,
    @IndexField @IndexFieldType(FieldType.TEXT) val categories: List<String>,
    @IndexField val authorName: String
)
