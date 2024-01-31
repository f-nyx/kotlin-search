package be.rlab.search.model

import be.rlab.nlp.model.Language
import be.rlab.search.Hashes.generateId
import java.util.*

/** Represents a document in the Lucene index.
 */
data class Document(
    /** Document id generated by [generateId] */
    val id: String,
    /** Document namespace that represents this collection. */
    val namespace: String,
    /** List of fields */
    val fields: List<Field<*>>,
    /** Document version, used to keep backward compatibility between releases. */
    val version: String,
    val score: Float
) {
    companion object {
        fun new(
            id: String,
            namespace: String,
            fields: List<Field<*>>,
            version: String,
            score: Float = 0.0f
        ): Document =
            Document(
                id = id,
                namespace = namespace,
                fields = fields,
                version = version,
                score = score
            )

        fun new(
            namespace: String,
            language: Language,
            fields: List<Field<*>>,
            version: String,
            score: Float = 0.0f
        ): Document =
            Document(
                id = generateId(UUID.randomUUID(), language),
                namespace = namespace,
                fields = fields,
                version = version,
                score = score
            )
    }

    @Suppress("UNCHECKED_CAST")
    fun getValues(fieldName: String): List<Any>? {
        return fields.find { field ->
            field.name == fieldName
        }?.let { field -> field.values as List<Any> }
    }

    inline operator fun<reified T> get(name: String): T? {
        val field = fields.find { field ->
            field.name == name
        }
        val targetClass = T::class
        return if (targetClass == List::class) {
            field?.values as T?
        } else {
            field?.values?.firstOrNull() as T?
        }
    }
}
