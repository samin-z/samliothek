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
    fun `web controllers and advice in adapter layer are internal`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.resideInPackage("..adapter.in.web..") }
            .filter {
                it.hasAnnotationOf(org.springframework.web.bind.annotation.RestController::class) ||
                    it.hasAnnotationOf(org.springframework.web.bind.annotation.RestControllerAdvice::class) ||
                    it.name.endsWith("Mapper")
            }.assertTrue { it.hasInternalModifier }
    }

    @Test
    fun `persistence adapters in adapter layer are internal`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.resideInPackage("..adapter.out..") }
            .filter { it.hasAnnotationOf(org.springframework.stereotype.Component::class) }
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
