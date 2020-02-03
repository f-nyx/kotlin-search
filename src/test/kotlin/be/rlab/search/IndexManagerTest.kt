package be.rlab.search

import be.rlab.nlp.model.Language
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.*

class IndexManagerTest {
    companion object {
        private const val NAMESPACE: String = "TestNamespace"
        const val FIELD_ID: String = "uuid"
        const val FIELD_HASH: String = "hash"
        const val FIELD_TITLE: String = "title"
        const val FIELD_DESCRIPTION: String = "description"
        const val FIELD_CATEGORY: String = "category"
        const val FIELD_AUTHOR_NAME: String = "author_name"
    }

    private val indexDir: File = File("./data/test-index")
    private lateinit var indexManager: IndexManager

    @Before
    fun setUp() {
        indexDir.deleteRecursively()
        indexManager = IndexManager(indexDir.absolutePath)
    }

    @Test
    fun terms() {
        indexManager.index(NAMESPACE, Language.SPANISH) {
            string(FIELD_ID, UUID.randomUUID().toString())
            int(FIELD_HASH, 1234)
            text(FIELD_TITLE, "Memorias del subsuelo")
            text(FIELD_DESCRIPTION, "Antihéroes de su ingente producción novelística") {
                store(false)
            }
            text(FIELD_AUTHOR_NAME, "Fiódor Dostoyevski") {
                store(false)
            }
            listOf("Drama", "Filosófico", "Psicológico").forEach { category ->
                text(FIELD_CATEGORY, category) {
                    store(false)
                }
            }
        }
        indexManager.sync()

        val results = indexManager.find(NAMESPACE, Language.SPANISH) {
            term(FIELD_TITLE, "Memorias")
        }.toList()

        assert(results.size == 1)
    }
}