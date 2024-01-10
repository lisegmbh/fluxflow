package de.lise.fluxflow.demo.springpizzaorder.menu

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/menu")
class MenuController(
    private val menuService: MenuService
) {
    @GetMapping
    fun getAll(): List<MenuItemDto> {
        return menuService.getAll()
            .map { MenuItemDto(it) }
    }
}