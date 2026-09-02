package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class KonsistArchitectureTests {
    @Test
    fun `service implementations in application layer are internal`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.resideInPackage("..application.service..") }
            .filter { it.name.endsWith("Service") }
            .assertTrue { it.hasInternalModifier }
    }

    @Test
    fun `use case interfaces expose exactly one public abstract method`() {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .filter { it.resideInPackage("..application.port.in..") }
            .assertTrue { iface ->
                iface.functions().count { it.hasPublicOrDefaultModifier } == 1
            }
    }

    @Test
    fun `no TODO or UnsupportedOperationException in overrides`() {
        Konsist
            .scopeFromProduction()
            .functions()
            .filter { it.hasOverrideModifier }
            .assertTrue { fn ->
                val text = fn.text
                !text.contains("TODO(") && !text.contains("UnsupportedOperationException")
            }
    }
}
