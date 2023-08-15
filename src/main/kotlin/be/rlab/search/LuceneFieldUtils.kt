package be.rlab.search

import be.rlab.search.LuceneIndex.Companion.DOC_VALUES_FIELD
import be.rlab.search.LuceneIndex.Companion.INDEXED_FIELD
import be.rlab.search.LuceneIndex.Companion.STORED_FIELD
import be.rlab.search.LuceneIndex.Companion.VERSION_FIELD
import be.rlab.search.model.Field
import be.rlab.search.model.FieldType
import org.apache.lucene.util.BytesRef
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexableField
import org.apache.lucene.document.Field as LuceneField
import org.apache.lucene.document.Document as LuceneDocument

object LuceneFieldUtils {
    const val PRIVATE_FIELD_PREFIX: String = "private!!"

    fun<T : Any> LuceneDocument.addField(
        field: Field<T>,
        version: String
    ) {
        require(field.values.isNotEmpty()) { "field value cannot be null" }

        if (field.docValues) {
            docValuesFromField(field).forEach(::add)
            newFields(field).map(::newStoredField).forEach(::add)
        } else {
            val newFields = newFields(field)
            if (field.stored) {
                newFields
                    .filter { newField -> newField.numericValue() != null }
                    .map(::newStoredField)
                    .forEach(::add)
            }
            newFields.forEach(::add)
        }

        add(
            StringField(
                privateField("${field.name}!!${LuceneIndex.TYPE_FIELD}", version),
                field.type.name,
                org.apache.lucene.document.Field.Store.YES
            )
        )
        add(StringField(privateField(VERSION_FIELD), version, LuceneField.Store.YES))
        add(StringField(privateField(STORED_FIELD), if (field.stored) "1" else "0", LuceneField.Store.YES))
        add(StringField(privateField(INDEXED_FIELD), if (field.indexed) "1" else "0", LuceneField.Store.YES))
        add(StringField(privateField(DOC_VALUES_FIELD), if (field.docValues) "1" else "0", LuceneField.Store.YES))
    }

    fun privateField(
        name: String,
        version: String = LuceneIndex.CURRENT_VERSION
    ): String {
        return when (version) {
            "1" -> name
            "2" -> "${PRIVATE_FIELD_PREFIX}$name"
            else -> throw RuntimeException("invalid document version: $version")
        }
    }

    fun IndexableField.booleanValue(): Boolean? {
        return stringValue()?.let { value ->
            when (value) {
                "0" -> false
                "1" -> true
                else -> false
            }
        }
    }

    private fun<T : Any> docValuesFromField(field: Field<T>): List<LuceneField> {
        require(field.docValues) { "the field is not marked as doc value" }
        require(field.values.isNotEmpty()) { "at least one value is required" }

        return when (field.type) {
            FieldType.STRING, FieldType.TEXT -> if (field.values.size > 1)
                field.values.map { SortedSetDocValuesField(field.name, BytesRef(it as String)) }
            else
                listOf(SortedDocValuesField(field.name, BytesRef(field.values.first() as String)))
            FieldType.INT, FieldType.LONG, FieldType.FLOAT, FieldType.DOUBLE -> if (field.values.size > 1) {
                toArray<Number>(field.values).map { value ->
                    SortedNumericDocValuesField(field.name, value.toLong())
                }
            } else
                listOf(NumericDocValuesField(field.name, (field.values.first() as Number).toLong()))
        }
    }

    private fun<T : Any> newFields(field: Field<T>): List<LuceneField> {
        return when (field.type) {
            FieldType.STRING -> toArray<String>(field.values).map { value ->
                StringField(field.name, value, if (field.stored) {
                    LuceneField.Store.YES
                } else {
                    LuceneField.Store.NO
                })
            }

            FieldType.TEXT -> toArray<String>(field.values).map { value ->
                TextField(field.name, value, if (field.stored) {
                    LuceneField.Store.YES
                } else {
                    LuceneField.Store.NO
                })
            }

            FieldType.INT -> listOf(IntPoint(field.name, *toArray<Int>(field.values).toIntArray()))
            FieldType.LONG -> listOf(LongPoint(field.name, *toArray<Long>(field.values).toLongArray()))
            FieldType.FLOAT -> listOf(FloatPoint(field.name, *toArray<Float>(field.values).toFloatArray()))
            FieldType.DOUBLE -> listOf(DoublePoint(field.name, *toArray<Double>(field.values).toDoubleArray()))
        }
    }

    private fun newStoredField(field: LuceneField): LuceneField {
        return field.numericValue()?.let { value ->
            when (field) {
                is IntPoint -> StoredField(field.name(), value.toInt())
                is LongPoint -> StoredField(field.name(), value.toLong())
                is FloatPoint -> StoredField(field.name(), value.toFloat())
                is DoublePoint -> StoredField(field.name(), value.toDouble())
                else -> field
            }
        } ?: field
    }

    private inline fun<reified T : Any> toArray(source: List<Any>): Array<T> {
        return source.map { item -> item as T }.toTypedArray()
    }
}
