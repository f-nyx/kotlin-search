package be.rlab.search.model

import be.rlab.nlp.model.Language
import be.rlab.search.LuceneIndex.Companion.CURRENT_VERSION
import be.rlab.search.annotation.IndexDocument
import be.rlab.search.annotation.IndexField
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

data class DocumentSchema<T : Any>(
    val namespace: String,
    val languages: List<Language>,
    val fields: List<FieldSchema>
) {
    companion object {

        fun<T : Any> new(
            namespace: String,
            languages: List<Language>,
            fields: List<FieldSchema>
        ): DocumentSchema<T> = DocumentSchema(
            namespace = namespace,
            languages = languages,
            fields = fields
        )

        fun<T : Any> fromClass(documentType: KClass<T>): DocumentSchema<T> {
            val docInfo = extractDocInfo(documentType)
            val fields = extractFields(documentType)

            return DocumentSchema(
                namespace = docInfo.namespace,
                languages = docInfo.languages.toList(),
                fields = fields
            )
        }

        private fun extractDocInfo(documentType: KClass<*>): IndexDocument {
            require(documentType.hasAnnotation<IndexDocument>()) {
                "@IndexDocument not found in class: ${documentType.qualifiedName}."
            }
            return documentType.findAnnotation()
                ?: throw RuntimeException("@IndexDocument annotation not found")
        }

        @Suppress("UNCHECKED_CAST")
        private fun<T : Any> extractFields(documentType: KClass<T>): List<FieldSchema> {
            require(documentType.hasAnnotation<IndexDocument>()) {
                "@IndexDocument not found in class: ${documentType.qualifiedName}."
            }

            return documentType.members
                .filter { member ->
                    member is KProperty1<*, *> && member.hasAnnotation<IndexField>()
                }
                .map { member ->
                    FieldSchema.from(member as KProperty1<Any, *>)
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun buildDocuments(source: T): List<Document> {
        val documentType = source::class
        val documentFields = fields.map { field ->
            val property = documentType.declaredMemberProperties.find { property ->
                property.name == field.propertyName
            } ?: throw RuntimeException(
                "property ${field.propertyName} not found in class ${documentType.qualifiedName}"
            )

            Field(
                name = field.name,
                value = (property as KProperty1<Any, *>).get(source) ?: throw NullPointerException(),
                type = field.type,
                stored = field.stored,
                indexed = field.indexed
            )
        }

        return languages.map { language ->
            Document.new(
                namespace = namespace,
                language = language,
                fields = documentFields,
                version = CURRENT_VERSION
            )
        }
    }
}
