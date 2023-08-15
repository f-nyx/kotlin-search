package be.rlab.search.model

import be.rlab.nlp.model.Language

data class DocumentSchema(
    val namespace: String,
    val languages: List<Language>,
    val fields: List<FieldSchema>
) {
    companion object {

        fun new(
            namespace: String,
            languages: List<Language>,
            fields: List<FieldSchema>
        ): DocumentSchema = DocumentSchema(
            namespace = namespace,
            languages = languages,
            fields = fields
        )
    }

    /** Finds a field schema by name or by the underlying property name.
     * @param name Field or property name.
     * @return the required field, or null if it does not exist.
     */
    fun findField(name: String): FieldSchema? {
        return fields.firstOrNull { field -> field.name == name || field.propertyName == name }
    }
}
