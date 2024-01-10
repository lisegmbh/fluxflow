package de.lise.fluxflow.demo.springpizzaorder.menu

import de.lise.fluxflow.demo.springpizzaorder.Price

data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Price
) {
    constructor(entity: MenuItemEntity) : this(
        entity.id,
        entity.name,
        entity.description,
        entity.price
    )
}