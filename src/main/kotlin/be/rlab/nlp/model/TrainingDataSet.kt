package be.rlab.nlp.model

import be.rlab.nlp.model.Language

data class TrainingDataSet(
    val language: Language,
    val categories: List<String>,
    val values: List<String>
)
