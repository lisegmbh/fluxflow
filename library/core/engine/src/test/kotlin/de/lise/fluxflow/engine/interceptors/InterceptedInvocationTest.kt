package de.lise.fluxflow.engine.interceptors

import de.lise.fluxflow.api.interceptors.FlowInterceptor
import de.lise.fluxflow.api.interceptors.InterceptionTokenStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InterceptedInvocationTest {

    @Test
    fun `the payload should be executed if all interceptors vote next`() {
        // Arrange
        val executionOrder = mutableListOf<String>()
        val tokenStates = mutableListOf<InterceptionTokenStatus>()
        var executed = false
        val l1 = FlowInterceptor<Any?> {
            executionOrder.add("1a")
            tokenStates.add(it.status)
            it.next()
            tokenStates.add(it.status)
            executionOrder.add("1b")
        }
        val l2 = FlowInterceptor<Any?> {
            executionOrder.add("2a")
            tokenStates.add(it.status)
            it.next()
            tokenStates.add(it.status)
            executionOrder.add("2b")
        }
        val l3 = FlowInterceptor<Any?> {
            executionOrder.add("3a")
            tokenStates.add(it.status)
            it.next()
            tokenStates.add(it.status)
            executionOrder.add("3b")
        }
        
        val invocation = InterceptedInvocation(
            listOf(
                l1,
                l2,
                l3
            )
        )
        
        // Act
        invocation.invoke(null) {
            executed = true
        }
        
        // Assert
        assertThat(executed).isTrue
        assertThat(executionOrder).containsExactly(
            "1a",
            "2a",
            "3a",
            "3b",
            "2b",
            "1b"
        )
        assertThat(tokenStates).containsExactly(
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Executed,
            InterceptionTokenStatus.Executed,
            InterceptionTokenStatus.Executed
        )
    }

    @Test
    fun `the payload should be executed if no interceptor votes abort`() {
        // Arrange
        val executionOrder = mutableListOf<String>()
        val tokenStates = mutableListOf<InterceptionTokenStatus>()
        var executed = false
        val l1 = FlowInterceptor<Any?> {
            executionOrder.add("1a")
            tokenStates.add(it.status)
            it.next()
            tokenStates.add(it.status)
            executionOrder.add("1b")
        }
        val l2 = FlowInterceptor<Any?> {
            executionOrder.add("2a")
            tokenStates.add(it.status)
            // No .next() call
        }
        val l3 = FlowInterceptor<Any?> {
            executionOrder.add("3a")
            tokenStates.add(it.status)
            it.next()
            tokenStates.add(it.status)
            executionOrder.add("3b")
        }

        val invocation = InterceptedInvocation(
            listOf(
                l1,
                l2,
                l3
            )
        )

        // Act
        invocation.invoke(null) {
            executed = true
        }

        // Assert
        assertThat(executed).isTrue
        assertThat(executionOrder).containsExactly(
            "1a",
            "2a",
            "3a",
            "3b",
            "1b"
        )
        assertThat(tokenStates).containsExactly(
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Executed,
            InterceptionTokenStatus.Executed
        )
    }

    @Test
    fun `the payload should be executed if no interceptor votes at all`() {
        // Arrange
        val executionOrder = mutableListOf<String>()
        val tokenStates = mutableListOf<InterceptionTokenStatus>()
        var executed = false
        val l1 = FlowInterceptor<Any?> {
            executionOrder.add("1a")
            tokenStates.add(it.status)
        }
        val l2 = FlowInterceptor<Any?> {
            executionOrder.add("2a")
            tokenStates.add(it.status)
        }
        val l3 = FlowInterceptor<Any?> {
            executionOrder.add("3a")
            tokenStates.add(it.status)
        }

        val invocation = InterceptedInvocation(
            listOf(
                l1,
                l2,
                l3
            )
        )

        // Act
        invocation.invoke(null) {
            executed = true
        }

        // Assert
        assertThat(executed).isTrue
        assertThat(executionOrder).containsExactly(
            "1a",
            "2a",
            "3a"
        )
        assertThat(tokenStates).containsExactly(
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Pending
        )
    }

    @Test
    fun `the payload should not be executed if any interceptor votes abort`() {
        // Arrange
        val executionOrder = mutableListOf<String>()
        val tokenStates = mutableListOf<InterceptionTokenStatus>()
        var executed = false
        val l1 = FlowInterceptor<Any?> {
            executionOrder.add("1a")
            tokenStates.add(it.status)
            it.next()
            tokenStates.add(it.status)
            executionOrder.add("1b")
        }
        val l2 = FlowInterceptor<Any?> {
            executionOrder.add("2a")
            tokenStates.add(it.status)
            it.abort()
            tokenStates.add(it.status)
            executionOrder.add("2b")
        }
        val l3 = FlowInterceptor<Any?> {
            executionOrder.add("3a")
            tokenStates.add(it.status)
            it.next()
            tokenStates.add(it.status)
            executionOrder.add("3b")
        }

        val invocation = InterceptedInvocation(
            listOf(
                l1,
                l2,
                l3
            )
        )

        // Act
        invocation.invoke(null) {
            executed = true
        }

        // Assert
        assertThat(executed).isFalse
        assertThat(executionOrder).containsExactly(
            "1a",
            "2a",
            "2b",
            "1b"
        )
        assertThat(tokenStates).containsExactly(
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Pending,
            InterceptionTokenStatus.Aborted,
            InterceptionTokenStatus.Aborted
        )
    }
}