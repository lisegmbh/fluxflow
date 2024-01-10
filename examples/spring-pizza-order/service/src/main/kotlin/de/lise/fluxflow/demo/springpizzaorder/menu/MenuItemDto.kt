package de.lise.fluxflow.demo.springpizzaorder.menu

import de.lise.fluxflow.demo.springpizzaorder.Price

data class MenuItemDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Price,
) {
    constructor(item: MenuItem) : this(
        item.id,
        item.name,
        item.description,
        item.price
    )
}