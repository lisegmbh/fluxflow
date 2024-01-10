package de.lise.fluxflow.demo.springpizzaorder.menu

import de.lise.fluxflow.demo.springpizzaorder.Price

data class MenuItemEntity(
    val id: String,
    val name: String,
    val description: String,
    val price: Price,
)