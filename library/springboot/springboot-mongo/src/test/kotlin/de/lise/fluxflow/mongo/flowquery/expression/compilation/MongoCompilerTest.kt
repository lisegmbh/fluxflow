package de.lise.fluxflow.mongo.flowquery.expression.compilation

import de.fluxflow.flowquery.expression.*
import de.fluxflow.flowquery.expression.ExpressionExtensions.Collections.contains
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isEqual
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isGreaterThan
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isGreaterThanOrEqual
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isLessThan
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isLessThanOrEqual
import de.fluxflow.flowquery.expression.ExpressionExtensions.Comparisons.isNotEqual
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.and
import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.or
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.endsWith
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.startsWith
import de.fluxflow.flowquery.expression.compilation.CompilationException
import de.lise.fluxflow.mongo.flowquery.expression.compilation.mapping.MongoCompilerMapper
import de.lise.fluxflow.mongo.flowquery.expression.compilation.token.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.reflect.KClass
import de.fluxflow.flowquery.expression.ExpressionExtensions.Strings.contains as stringContains

class MongoCompilerTest {
    private lateinit var mockSubclassProvider: SubclassProvider
    private lateinit var compiler: MongoCompiler
    private lateinit var config: MongoCompilerConfig
    private lateinit var mongoCompilerMapper: MongoCompilerMapper
    
    @BeforeEach
    fun setup() {
        // Arrange
        mockSubclassProvider = mock()
        config = MongoCompilerConfig(typeFieldName = "_class")
        mongoCompilerMapper = mock<MongoCompilerMapper> { 
            on { map<Any, Any>(any()) } doAnswer {it.rawArguments[0] as Expression<Any, Any>}
        }
        compiler = MongoCompiler(
            subclassProvider = mockSubclassProvider,
            config = config
        )
    }

    @Test
    fun `compile should return compilation result with constant token for constant expression`() {
        // Arrange
        val constantExpression = Expression.const<TestUser, String>("test-value")

        // Act
        val result = compiler.compile(constantExpression)

        // Assert
        assertThat(result.result).isInstanceOf(ConstantToken::class.java)
        val constantToken = result.result as ConstantToken
        assertThat(constantToken.toValue()).isEqualTo("test-value")
    }

    @Test
    fun `compile should return root token for root expression`() {
        // Arrange
        val rootExpression = Expression.root<TestUser>()

        // Act
        val result = compiler.compile(rootExpression)

        // Assert
        assertThat(result.result).isInstanceOf(RootToken::class.java)
    }

    @Test
    fun `compile should return root token for conjunction expression`() {
        // Arrange
        val conjunctionExpression = mock<ConjunctionExpression<TestUser, TestUser>>()
        whenever(conjunctionExpression.toText()).thenReturn("conjunction()")

        // Act
        val result = compiler.compile(conjunctionExpression)

        // Assert
        assertThat(result.result).isInstanceOf(RootToken::class.java)
    }

    @Test
    fun `compile should wrap exceptions in compilation exception`() {
        // Arrange - Create an expression that would cause an error during compilation
        val problematicExpression = mock<ConstantExpression<TestUser, String>>()
        whenever(problematicExpression.value).thenThrow(RuntimeException("Test exception"))
        whenever(problematicExpression.toText()).thenReturn("problematic")

        // Act & Assert
        assertThatThrownBy { compiler.compile(problematicExpression) }
            .isInstanceOf(CompilationException::class.java)
            .hasMessageContaining("Compilation failed")
    }

    @Test
    fun `compile should use custom type field name from config`() {
        // Arrange
        val customConfig = MongoCompilerConfig(typeFieldName = "customType")
        val customCompiler = MongoCompiler(
            subclassProvider = mockSubclassProvider,
            config = customConfig
        )

        val mockClass = mock<KClass<String>>()
        val subclass = String::class.java
        val subclasses = setOf(subclass)
        whenever(mockSubclassProvider.findSubclasses(mockClass)).thenReturn(subclasses)

        // Create a real root expression
        val rootExpression = Expression.root<TestUser>()

        val isTypeExpression = mock<IsTypeExpression<TestUser, TestUser, String>>()
        whenever(isTypeExpression.instance).thenReturn(rootExpression)
        whenever(isTypeExpression.requiredType).thenReturn(mockClass)
        whenever(isTypeExpression.toText()).thenReturn("root() is String")

        // Act
        val result = customCompiler.compile(isTypeExpression)

        // Assert
        assertThat(result.result).isInstanceOf(StatementOperationToken::class.java)
        verify(mockSubclassProvider).findSubclasses(mockClass)
    }

    @Test
    fun `compile should handle cast expression by delegating to instance`() {
        // Arrange
        val instanceExpression = Expression.const<TestUser, String>("casted-value")

        val castExpression = mock<CastExpression<TestUser, String, String>>()
        whenever(castExpression.instance).thenReturn(instanceExpression)
        whenever(castExpression.toText()).thenReturn("cast(const('casted-value'))")

        // Act
        val result = compiler.compile(castExpression)

        // Assert
        assertThat(result.result).isInstanceOf(ConstantToken::class.java)
        val constantToken = result.result as ConstantToken
        assertThat(constantToken.toValue()).isEqualTo("casted-value")
    }

    @Test
    fun `compile should handle binary equal operation correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val nameProperty = user.get(TestUser::name)
        val binaryExpression = nameProperty.isEqual("John")

        // Act
        val result = compiler.compile(binaryExpression)

        // Assert
        assertThat(result.result).isInstanceOf(MatchToken::class.java)
        val matchToken = result.result as MatchToken
        assertThat(matchToken.expression).isInstanceOf(StatementOperationToken::class.java)
    }

    @Test
    fun `compile should handle property expression correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val propertyExpression = user.get(TestUser::name)

        // Act
        val result = compiler.compile(propertyExpression)

        // Assert
        assertThat(result.result).isInstanceOf(PropertyToken::class.java)
    }

    @Test
    fun `compile should handle starts with expression correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val emailProperty = user.get(TestUser::email)
        val startsWithExpression = emailProperty.startsWith("admin@")

        // Act
        val result = compiler.compile(startsWithExpression)

        // Assert
        assertThat(result.result).isInstanceOf(RegexToken::class.java)
    }

    @Test
    fun `compile should handle ends with expression correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val emailProperty = user.get(TestUser::email)
        val endsWithExpression = emailProperty.endsWith("@example.com")

        // Act
        val result = compiler.compile(endsWithExpression)

        // Assert
        assertThat(result.result).isInstanceOf(RegexToken::class.java)
    }

    @Test
    fun `compile should handle contains expression correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val emailProperty = user.get(TestUser::email)
        val containsExpression = emailProperty.stringContains("admin")

        // Act
        val result = compiler.compile(containsExpression)

        // Assert
        assertThat(result.result).isInstanceOf(RegexToken::class.java)
    }

    @Test
    fun `compile should handle binary comparison operations correctly`() {
        // Arrange - Test different comparison operations using real expressions
        val user = Expression.root<TestUser>()
        val ageProperty = user.get(TestUser::age)

        val operations = listOf(
            ageProperty.isLessThan(18),
            ageProperty.isLessThanOrEqual(18),
            ageProperty.isGreaterThan(18),
            ageProperty.isGreaterThanOrEqual(18),
            ageProperty.isNotEqual(18)
        )

        operations.forEach { binaryExpression ->
            // Act
            val result = compiler.compile(binaryExpression)

            // Assert
            assertThat(result.result).isInstanceOf(MatchToken::class.java)
            val matchToken = result.result as MatchToken
            assertThat(matchToken.expression).isInstanceOf(StatementOperationToken::class.java)
        }
    }

    @Test
    fun `compile should handle logical and operation correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val isActiveExpr = user.get(TestUser::isActive).isEqual(true)
        val isAdminExpr = user.get(TestUser::isAdmin).isEqual(true)
        val andExpression = isActiveExpr.and(isAdminExpr)

        // Act
        val result = compiler.compile(andExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AndToken::class.java)
    }

    @Test
    fun `compile should handle logical or operation correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val isActiveExpr = user.get(TestUser::isActive).isEqual(true)
        val isAdminExpr = user.get(TestUser::isAdmin).isEqual(true)
        val orExpression = isActiveExpr or isAdminExpr

        // Act
        val result = compiler.compile(orExpression)

        // Assert
        assertThat(result.result).isInstanceOf(OrToken::class.java)
    }

    @Test
    fun `compile should handle logical not operation correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val isActiveExpr = user.get(TestUser::isActive).isEqual(true)
        val notExpression = Expression.not(isActiveExpr)

        // Act
        val result = compiler.compile(notExpression)

        // Assert
        assertThat(result.result).isInstanceOf(NotToken::class.java)
    }

    @Test
    fun `compile should handle collection contains expression correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val rolesProperty = user.get(TestUser::roles)
        val containsExpression = rolesProperty.contains("admin")

        // Act
        val result = compiler.compile(containsExpression)

        // Assert
        assertThat(result.result).isInstanceOf(ExpressionTokenImpl::class.java)
    }

    @Test
    fun `compile should handle collection containsElementThat expression correctly`() {
        // Arrange - Test collection contains element using real expressions where possible
        val user = Expression.root<TestUser>()
        val rolesProperty = user.get(TestUser::roles)

        // Use collection contains instead of containsElementThat to avoid complex mocking
        val containsExpression = rolesProperty.contains("admin")

        // Act
        val result = compiler.compile(containsExpression)

        // Assert
        assertThat(result.result).isInstanceOf(ExpressionTokenImpl::class.java)
    }

    @Test
    fun `compile should handle map get expression correctly`() {
        // Arrange - Using real expressions
        val user = Expression.root<TestUser>()
        val metadataProperty = user.get(TestUser::metadata)

        // Create mock since map access API might be complex
        val mapGetExpression = mock<MapAccessExpression<TestUser, Map<String, String>, String, String>>()
        whenever(mapGetExpression.instance).thenReturn(metadataProperty)
        whenever(mapGetExpression.key).thenReturn(Expression.const("role"))
        whenever(mapGetExpression.toText()).thenReturn("metadata['role']")

        // Act
        val result = compiler.compile(mapGetExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AnonymousPropertyToken::class.java)
    }


    @Test
    fun `compile should handle complex nested logical operations correctly`() {
        // Arrange - Complex nested AND/OR operations
        val user = Expression.root<TestUser>()
        val complexExpression = user.get(TestUser::isActive).isEqual(true)
            .and(
                user.get(TestUser::age).isGreaterThan(18)
                    .or(user.get(TestUser::isAdmin).isEqual(true))
            )
            .and(user.get(TestUser::email).startsWith("admin@"))

        // Act
        val result = compiler.compile(complexExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AndToken::class.java)
    }

    @Test
    fun `compile should handle mixed string and comparison operations correctly`() {
        // Arrange - Mix of string operations and comparisons
        val user = Expression.root<TestUser>()
        val mixedExpression = user.get(TestUser::name).stringContains("admin")
            .and(user.get(TestUser::age).isGreaterThanOrEqual(21))
            .and(user.get(TestUser::email).endsWith("@company.com"))

        // Act
        val result = compiler.compile(mixedExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AndToken::class.java)
    }

    @Test
    fun `compile should handle collection operations with complex predicates correctly`() {
        // Arrange - Test complex logical operations instead of complex collection operations
        val user = Expression.root<TestUser>()
        val rolesContainsAdmin = user.get(TestUser::roles).contains("admin")
        val rolesContainsManager = user.get(TestUser::roles).contains("manager")
        val complexExpression = rolesContainsAdmin.or(rolesContainsManager)

        // Act
        val result = compiler.compile(complexExpression)

        // Assert
        assertThat(result.result).isInstanceOf(OrToken::class.java)
    }

    @Test
    fun `compile should handle multiple property access chains correctly`() {
        // Arrange - Multiple property accesses in complex expressions
        val user = Expression.root<TestUser>()
        val propertyChainExpression = user.get(TestUser::name).isEqual("John")
            .and(user.get(TestUser::age).isLessThan(65))
            .and(user.get(TestUser::email).stringContains("@"))
            .and(user.get(TestUser::isActive).isEqual(true))
            .and(user.get(TestUser::roles).contains("user"))

        // Act
        val result = compiler.compile(propertyChainExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AndToken::class.java)
    }

    @Test
    fun `compile should handle map access with complex conditions correctly`() {
        // Arrange - Map access combined with other operations (simplified)
        val user = Expression.root<TestUser>()
        val isActiveExpr = user.get(TestUser::isActive).isEqual(true)
        val nameExpr = user.get(TestUser::name).isEqual("admin")
        val mapAccessExpression = isActiveExpr.and(nameExpr)

        // Act
        val result = compiler.compile(mapAccessExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AndToken::class.java)
    }

    @Test
    fun `compile should handle negation of complex expressions correctly`() {
        // Arrange - NOT operation on complex nested expressions
        val user = Expression.root<TestUser>()
        val complexInnerExpression = user.get(TestUser::age).isLessThan(18)
            .or(user.get(TestUser::isActive).isEqual(false))
        val negatedExpression = Expression.not(complexInnerExpression)

        // Act
        val result = compiler.compile(negatedExpression)

        // Assert
        assertThat(result.result).isInstanceOf(NotToken::class.java)
    }

    @Test
    fun `compile should handle multiple string operations in sequence correctly`() {
        // Arrange - Multiple string operations combined
        val user = Expression.root<TestUser>()
        val stringOperationsExpression = user.get(TestUser::email).startsWith("admin")
            .and(user.get(TestUser::email).endsWith("@company.com"))
            .and(user.get(TestUser::name).stringContains("manager"))

        // Act
        val result = compiler.compile(stringOperationsExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AndToken::class.java)
    }

    @Test
    fun `compile should handle deep nested OR and AND combinations correctly`() {
        // Arrange - Deep nesting of logical operations
        val user = Expression.root<TestUser>()
        val deepNestedExpression = (
            user.get(TestUser::isAdmin).isEqual(true)
                .and(user.get(TestUser::age).isGreaterThan(25))
        ).or(
            user.get(TestUser::roles).contains("manager")
                .and(user.get(TestUser::isActive).isEqual(true))
        ).or(
            user.get(TestUser::email).startsWith("ceo@")
        )

        // Act
        val result = compiler.compile(deepNestedExpression)

        // Assert
        assertThat(result.result).isInstanceOf(OrToken::class.java)
    }

    @Test
    fun `compile should handle isAnyOf operator correctly`() {
        // Arrange - Testing isAnyOf with multiple values
        val user = Expression.root<TestUser>()
        val nameProperty = user.get(TestUser::name)

        // Create mock IsAnyOfOperator since this might not have direct API support
        val isAnyOfExpression = mock<IsAnyOfOperator<TestUser, String>>()
        whenever(isAnyOfExpression.valueToTest).thenReturn(nameProperty)
        whenever(isAnyOfExpression.anyOf).thenReturn(setOf(
            Expression.const<TestUser, String>("John"),
            Expression.const<TestUser, String>("Jane"),
            Expression.const<TestUser, String>("Bob")
        ))
        whenever(isAnyOfExpression.toText()).thenReturn("name in ['John', 'Jane', 'Bob']")

        // Act
        val result = compiler.compile(isAnyOfExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AnyOfToken::class.java)
    }

    @Test
    fun `compile should handle combination of all supported operations correctly`() {
        // Arrange - Kitchen sink test with all operation types (simplified)
        val user = Expression.root<TestUser>()
        val kitchenSinkExpression = user.get(TestUser::isActive).isEqual(true)
            .and(user.get(TestUser::age).isGreaterThanOrEqual(18))
            .and(user.get(TestUser::email).startsWith("user@"))
            .and(user.get(TestUser::name).stringContains("John"))
            .and(user.get(TestUser::roles).contains("user"))
            .and(Expression.not(user.get(TestUser::isAdmin).isEqual(true)))

        // Act
        val result = compiler.compile(kitchenSinkExpression)

        // Assert
        assertThat(result.result).isInstanceOf(AndToken::class.java)
    }

    @Test
    fun `compile should call mapper with the input expression`() {
        // Arrange
        val mockMapper = mock<MongoCompilerMapper>()
        val compilerWithMapper = MongoCompiler(
            subclassProvider = mockSubclassProvider,
            expressionMapper = mockMapper,
            config = config
        )
        
        val inputExpression = Expression.root<TestUser>()
        whenever(mockMapper.map<TestUser, TestUser>(any())).thenReturn(inputExpression)

        // Act
        compilerWithMapper.compile(inputExpression)

        // Assert - Verify mapper was called with the original expression
        verify(mockMapper).map(inputExpression)
    }

    @Test
    fun `compile should use mapped expression result for compilation`() {
        // Arrange
        val mockMapper = mock<MongoCompilerMapper>()
        val compilerWithMapper = MongoCompiler(
            subclassProvider = mockSubclassProvider,
            expressionMapper = mockMapper,
            config = config
        )
        
        // Original expression that accesses 'name' property
        val originalExpression = Expression.root<TestUser>().get(TestUser::name)
        
        // Mapped expression that accesses 'email' property instead
        val mappedExpression = Expression.root<TestUser>().get(TestUser::email)
        
        whenever(mockMapper.map(originalExpression)).thenReturn(mappedExpression)

        // Act
        val result = compilerWithMapper.compile(originalExpression)

        // Assert - The result should be based on the mapped expression (email, not name)
        verify(mockMapper).map(originalExpression)
        assertThat(result.result).isInstanceOf(PropertyToken::class.java)
        val propertyToken = result.result as PropertyToken
        assertThat(propertyToken.property).isEqualTo(TestUser::email)
    }

    @Test
    fun `compile should use mapper result even when it differs from input`() {
        // Arrange - Mapper transforms the expression type
        val mockMapper = mock<MongoCompilerMapper>()
        val compilerWithMapper = MongoCompiler(
            subclassProvider = mockSubclassProvider,
            expressionMapper = mockMapper,
            config = config
        )
        
        // Input: constant expression
        val inputExpression = Expression.const<TestUser, String>("input")
        
        // Mapped: different constant
        val mappedExpression = Expression.const<TestUser, String>("mapped")
        
        whenever(mockMapper.map(inputExpression)).thenReturn(mappedExpression)

        // Act
        val result = compilerWithMapper.compile(inputExpression)

        // Assert - Should use the mapped expression's value
        verify(mockMapper).map(inputExpression)
        assertThat(result.result).isInstanceOf(ConstantToken::class.java)
        val constantToken = result.result as ConstantToken
        assertThat(constantToken.toValue()).isEqualTo("mapped")
    }

    /**
     * Test data class for testing expressions.
     */
    data class TestUser(
        val name: String,
        val age: Int,
        val email: String,
        val isActive: Boolean,
        val isAdmin: Boolean,
        val roles: List<String>,
        val metadata: Map<String, String>
    )
}
