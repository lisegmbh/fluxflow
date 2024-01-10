package de.lise.fluxflow.demo.springpizzaorder.menu

import org.springframework.stereotype.Service

@Service
class MenuService(
    private val menuRepository: MenuRepository
) {
    fun getAll(): List<MenuItem> {
        return menuRepository.findAll()
            .map { MenuItem(it) }
    }

    fun get(id: String): MenuItem {
        return MenuItem(menuRepository.getById(id))
    }
}