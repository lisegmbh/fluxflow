package de.lise.fluxflow.demo.springpizzaorder.order

import de.lise.fluxflow.demo.springpizzaorder.cart.Cart

data class OrderWorkflow(
    val cart: Cart
)