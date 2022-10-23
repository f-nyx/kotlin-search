package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.model.TypedSearchResult
import be.rlab.search.query.range
import be.rlab.search.query.term
import be.rlab.search.query.wildcard
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class IndexManagerTest {
    companion object {
        const val NAMESPACE: String = "TestNamespace"
        const val FIELD_ID: String = "uuid"
        const val FIELD_HASH: String = "hash"
        const val FIELD_TITLE: String = "title"
        const val FIELD_DESCRIPTION: String = "description"
        const val FIELD_CATEGORY: String = "category"
        const val FIELD_AUTHOR_NAME: String = "author_name"
    }

    private val indexDir: File = File("./data/test-index")
    private lateinit var indexManager: IndexManager

    @BeforeEach
    fun setUp() {
        indexDir.deleteRecursively()
        indexManager = IndexManager(indexDir.absolutePath)
    }

    @AfterEach
    fun tearDown() {
        indexManager.close()
    }

    @Test
    fun terms() {
        indexManager.index(NAMESPACE, Language.SPANISH) {
            string(FIELD_ID, UUID.randomUUID().toString())
            int(FIELD_HASH, 1234)
            text(FIELD_TITLE, "Memorias del subsuelo")
            text(FIELD_DESCRIPTION, "Antihéroes de su ingente producción novelística") {
                store(true)
            }
            text(FIELD_AUTHOR_NAME, "Fiódor Dostoyevski") {
                store(true)
            }
            listOf("Drama", "Filosófico", "Psicológico").forEach { category ->
                text(FIELD_CATEGORY, category) {
                    store(true)
                }
            }
        }
        indexManager.sync()

        val results = indexManager.find(NAMESPACE, Language.SPANISH) {
            term(FIELD_TITLE, "Memorias")
        }.toList()

        assert(results.size == 1)
    }

    @Test
    fun mapper() {
        val book = Book(
            id = UUID.randomUUID().toString(),
            hash = 1337,
            title = "Memorias del subsuelo",
            description = "Antihéroes de su ingente producción novelística",
            authorName = "Fiódor Dostoyevski",
            category = "Drama"
        )
        val mapper = IndexMapper(indexManager)
        mapper.index(book)
        indexManager.sync()

        val results1: TypedSearchResult<Book> = mapper.search(Language.SPANISH) {
            term("Memorias")
        }
        val results2: TypedSearchResult<Book> = mapper.search(Language.SPANISH) {
            term(Book::title, "Memorias")
        }
        val results3: TypedSearchResult<Book> = mapper.search(Language.SPANISH) {
            term(FIELD_TITLE, "Memorias")
            term("drama")
        }

        assert(results1.docs.size == 1)
        assert(results2.docs.size == 1)
        assert(results3.docs.size == 1)
        assert(results1.docs.first() == book.copy(title = null))
        assert(results2.docs.first() == book.copy(title = null))
        assert(results3.docs.first() == book.copy(title = null))
    }
}
