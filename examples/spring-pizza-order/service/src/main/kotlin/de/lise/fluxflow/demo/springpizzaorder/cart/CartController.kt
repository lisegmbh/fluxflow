package de.lise.fluxflow.demo.springpizzaorder.cart

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.demo.springpizzaorder.menu.MenuItemDto
import de.lise.fluxflow.demo.springpizzaorder.menu.MenuService
import de.lise.fluxflow.demo.springpizzaorder.order.OrderWorkflow
import de.lise.fluxflow.demo.springpizzaorder.workflow.order.EditCartStep
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cart")
class CartController(
    private val workflowStarterService: WorkflowStarterService,
    private val menuService: MenuService
) {

    private fun toCart(workflow: Workflow<OrderWorkflow>): CartDto {
        return CartDto(
            workflow.id.value,
            workflow.model.cart.items.map {
                CartItemDto(
                    MenuItemDto(menuService.get(it.itemId)),
                    it.amount
                )
            }
        )
    }

    @PostMapping
    fun create(@RequestBody cart: CreateCartDto): CartDto {
        val order = OrderWorkflow(
            Cart(
                cart.items.map { it.toCartItem() }.toList()
            )
        )
        val result = workflowStarterService.start(
            order,
            Continuation.step(EditCartStep(order))
        )

        return toCart(result)
    }
}