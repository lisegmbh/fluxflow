package de.lise.fluxflow.demo.springpizzaorder.cart

import de.lise.fluxflow.demo.springpizzaorder.menu.MenuItemDto

data class CartItemDto(
    val item: MenuItemDto,
    val amount: Int
) {
    fun toCartItem(): CartItem {
        return CartItem(item.id, amount)
    }
}