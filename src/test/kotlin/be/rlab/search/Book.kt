package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.annotation.*
import be.rlab.search.model.FieldType

@IndexDocument(namespace = IndexManagerTest.NAMESPACE, languages = [Language.SPANISH, Language.ENGLISH])
data class Book(
    @IndexField @IndexFieldType(FieldType.STRING) val id: String,
    @IndexField @Indexed val hash: Int,
    @IndexField @Stored(false) val title: String?,
    @IndexField val description: String,
    @IndexField val category: String,
    @IndexField val authorName: String
)
