package de.lise.fluxflow.demo.springpizzaorder.menu

import de.lise.fluxflow.demo.springpizzaorder.Price
import org.springframework.stereotype.Repository

@Repository
class MenuRepository {
    private val all = listOf(
        MenuItemEntity(
            "p-magaritha",
            "Pizza Margherita",
            "A classic Italian masterpiece featuring a thin, crispy crust topped with " +
                    "rich tomato sauce, fresh mozzarella, and aromatic basil leaves. Simple, yet bursting with " +
                    "authentic flavors.",
            Price(10.0)
        ),
        MenuItemEntity(
            "p-funghi",
            "Pizza Funghi",
            "A savory blend of mushrooms, tomato sauce, and melted mozzarella on a thin crust. Pure delight " +
                    "for mushroom and pizza enthusiasts alike.",
            Price(10.50)
        ),
        MenuItemEntity(
            "p-hawaiian",
            "Hawaiian Pizza",
            "Pineapple, ham, and mozzarella—because some pizzas just want to watch the world debate. Love it " +
                    "or leave it, this tropical twist is a slice of sweet rebellion.",
            Price(4.20)
        )
    )

    fun findAll(): List<MenuItemEntity> {
        return all
    }

    fun getById(id: String): MenuItemEntity {
        return all.first { it.id == id }
    }
}