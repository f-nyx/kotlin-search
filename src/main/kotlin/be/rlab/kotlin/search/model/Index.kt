package be.rlab.kotlin.search.model

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter

/** Represents a Lucene index.
 *
 * @param language Index language.
 * @param analyzer Analyzer used to pre-process text.
 * @param indexReader Reader to query the index.
 * @param indexWriter Writer to index and to store data into the index.
 */
data class Index(
    val language: Language,
    val analyzer: Analyzer,
    val indexReader: IndexReader,
    val indexWriter: IndexWriter
) {
    /** Updates the index reader.
     * @param indexReader New index reader.
     * @return the updated index.
     */
    fun update(indexReader: IndexReader): Index = copy(
        indexReader = indexReader
    )
}
