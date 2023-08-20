package be.rlab.search.mock

import be.rlab.search.Book
import io.github.serpro69.kfaker.faker
import java.util.UUID

val faker = faker {}

data class TestBook(
    val id: String = UUID.randomUUID().toString(),
    val title: String = faker.book.title(),
    val description: String = faker.bojackHorseman.quotes(),
    val genre: String = faker.book.genre(),
    val categories: List<String> = listOf(faker.adjective.positive(), faker.adjective.positive()),
    val author: String = faker.book.author(),
    val hash: Int = faker.random.nextInt(),
    val rate: Float = faker.random.nextFloat()
) {
    fun new(): Book = Book(
        id = id,
        title = title,
        description = description,
        genre = genre,
        categories = categories,
        author = author,
        hash = hash,
        rate = rate
    )
}
