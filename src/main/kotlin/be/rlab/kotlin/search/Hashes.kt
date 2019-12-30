package be.rlab.kotlin.search

import be.rlab.kotlin.search.model.Language
import org.apache.commons.codec.digest.MurmurHash3
import java.util.*

/** Utility to generate and read hashes.
 */
object Hashes {
    private val languageHashes: Map<Language, String> = Language.values().map { language ->
        language to Integer.toHexString(MurmurHash3.hash32(language.name)).padStart(8, '0')
    }.toMap()
    private val reverseLanguageHashes: Map<String, Language> = Language.values().map { language ->
        Integer.toHexString(MurmurHash3.hash32(language.name)).padStart(8, '0') to language
    }.toMap()

    /** Generates a non-cryptographic, language-dependant hash to represent unique identifiers.
     * It uses a combination of murmur3 hashes over the language, the id and the current time.
     * The generated id contains information about the language and it can be reversed using [getLanguage].
     */
    fun generateId(
        id: UUID,
        language: Language
    ): String {
        val langHash: String = languageHashes.getValue(language)
        val timestamp: String = java.lang.Long.toHexString(System.currentTimeMillis()).padStart(12, '0')
        val idHash: String = Integer.toHexString(MurmurHash3.hash32(id.toString())).padStart(8, '0')
        return "$langHash$timestamp$idHash"
    }

    /** Returns the language for an identifier generated with [generateId].
     * @param id Id to retrieve language.
     * @return The id language.
     */
    fun getLanguage(id: String): Language {
        return reverseLanguageHashes.getValue(id.substring(0..7))
    }
}