package be.rlab.search

import be.rlab.nlp.model.Language
import be.rlab.search.mock.TestBook
import be.rlab.search.model.TypedSearchResult
import be.rlab.search.query.sortBy
import be.rlab.search.query.term
import be.rlab.support.SearchTestUtils.firstWord
import org.apache.lucene.search.similarities.BM25Similarity
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
        indexManager = IndexManager.Builder(indexDir.absolutePath)
            .forLanguages(Language.entries)
            .withSimilarity(BM25Similarity())
            .build()
    }

    @AfterEach
    fun tearDown() {
        indexManager.close()
    }

    @Test
    fun terms() {
        indexManager.addSchema(NAMESPACE) {
            string(FIELD_ID) {
                docValues()
            }
            int(FIELD_HASH) {
                index()
                store()
            }
            text(FIELD_TITLE)
            text(FIELD_DESCRIPTION)
            text(FIELD_AUTHOR_NAME)
            text(FIELD_CATEGORY) {
                docValues()
            }
        }

        indexManager.index(NAMESPACE, Language.SPANISH) {
            string(FIELD_ID, UUID.randomUUID().toString())
            int(FIELD_HASH, 1234)
            field(FIELD_TITLE, "Memorias del subsuelo")
            text(FIELD_DESCRIPTION, "Antihéroes de su ingente producción novelística") {
                store()
            }
            text(FIELD_AUTHOR_NAME, "Fiódor Dostoyevski") {
                store()
            }
            listOf("Drama", "Filosófico", "Psicológico").forEach { category ->
                text(FIELD_CATEGORY, category) {
                    store()
                }
            }
        }
        indexManager.sync()

        val results1 = indexManager.search(NAMESPACE, Language.SPANISH) {
            term("Memorias")
            sortBy(FIELD_ID)
        }
        val results2 = indexManager.search(NAMESPACE, Language.SPANISH) {
            term(FIELD_TITLE, "Memorias")
            term("drama") {
                by(FIELD_DESCRIPTION, FIELD_CATEGORY)
            }
        }
        val results3 = indexManager.search(NAMESPACE, Language.SPANISH) {
            term("drama") {
                by(FIELD_DESCRIPTION)
            }
        }

        assert(results1.docs.size == 1)
        assert(results2.docs.size == 1)
        assert(results3.docs.isEmpty())
    }

    @Test
    fun mapper() {
        val books = Array(10) { TestBook().new() }
        val mapper = IndexMapper(indexManager)

        books[4] = books[4].copy(title = "memorias")
        books[8] = books[8].copy(title = "subsuelo", categories = listOf("drama"))
        books.forEach { book -> mapper.index(book, Language.ENGLISH) }
        indexManager.sync()

        val results1: TypedSearchResult<Book> = mapper.search(Language.ENGLISH) {
            term(firstWord(books[2].description))
            sortBy(Book::genre)
        }
        val results2: TypedSearchResult<Book> = mapper.search(Language.ENGLISH) {
            term(Book::title, books[4].title!!)
        }
        val results3: TypedSearchResult<Book> = mapper.search(Language.ENGLISH) {
            term(FIELD_TITLE, books[8].title!!)
            term(books[8].categories.first())
        }

        assert(results1.docs.isNotEmpty())
        assert(results2.docs.isNotEmpty())
        assert(results3.docs.isNotEmpty())
    }
}
