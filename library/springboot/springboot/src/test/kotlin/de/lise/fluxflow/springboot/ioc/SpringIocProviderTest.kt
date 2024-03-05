package de.lise.fluxflow.springboot.ioc

import de.lise.fluxflow.springboot.ioc.testservices.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        SpringIocProvider::class,
        SpringIocProviderTestService::class,
        BaseServiceImpl::class,
        ExtendingServiceImpl::class,
        DuplicateServiceImpl1::class,
        DuplicateServiceImpl2::class
    ]
)
class SpringIocProviderTest {
    @Autowired
    var springIocProvider: SpringIocProvider? = null

    @Autowired
    var baseService: BaseService? = null

    @Test
    fun `provide should return the requested dependency if it is available`() {
        // Act
        val resolution = springIocProvider!!.provide(SpringIocProviderTestService::class)

        // Assert
        assertThat(resolution).isNotNull
    }

    @Test
    fun `provide should return the correct bean if the requested dependency is available in multiple versions`() {
        // Act
        val resolution = springIocProvider!!.provide(BaseService::class)

        // Assert
        assertThat(resolution).isNotNull
        assertThat(resolution).isSameAs(baseService)
        assertThat(resolution).isInstanceOf(BaseServiceImpl::class.java)
    }

    @Test
    fun `provide should throw an exception if more than one bean is eligible for the requested dependency`() {
        // Assert & Act
        assertThrows<NoUniqueBeanDefinitionException> {
            springIocProvider!!.provide(DuplicateService::class)
        }
    }

    @Test
    fun `provide should return null if the requested dependency is not available`() {
        // Act
        val resolution = springIocProvider!!.provide(SomeNonExistingDependency::class)

        // Assert
        assertThat(resolution).isNull()
    }

    class SomeNonExistingDependency
}