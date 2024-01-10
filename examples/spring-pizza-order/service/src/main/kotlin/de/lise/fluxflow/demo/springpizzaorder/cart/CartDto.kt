package de.lise.fluxflow.demo.springpizzaorder.cart

data class CartDto(
    val id: String,
    val items: List<CartItemDto>
)