package be.rlab.kotlin.search.model

data class TrainingDataSet(
    val language: Language,
    val categories: List<String>,
    val values: List<String>
)
