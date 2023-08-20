package be.rlab.training

import be.rlab.nlp.model.Language
import be.rlab.support.csv.ParserConfig
import java.io.File

/** Represents a file-system based dataset.
 */
data class DataSet(
    /** Namespace to scope this dataset in the index. */
    val namespace: String,
    /** Dataset language. */
    val language: Language,
    /** A classifier to group this dataset. */
    val classifier: String,
    /** Dataset file. */
    val file: File,
    /* CSV parser config. */
    val parserConfig: ParserConfig = ParserConfig.default()
)
