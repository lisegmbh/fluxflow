package de.lise.fluxflow.reflection.record

data class PersonTestModel(
    val firstname: String,
    val children: List<PersonTestModel>
)