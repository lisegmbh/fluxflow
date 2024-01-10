package de.lise.fluxflow.demo.springpizzaorder.cart

data class CreateCartDto(
    val items: List<CartItemDto>
)