package architecture

import com.samliothek.SamliothekApplication
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.docs.Documenter

class ArchitectureTests {
    private val modules = ApplicationModules.of(SamliothekApplication::class.java)

    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.samliothek")

    @Test
    fun `modulith modules verify — no illegal cross-module access or cycles`() {
        modules.verify()
    }

    @Test
    fun `domain is free of framework dependencies`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta..",
                "tools.jackson..",
                "..adapter..",
                "..application..",
            ).allowEmptyShould(true)
            .check(classes)
    }

    @Test
    fun `adapters are not referenced by inner rings`() {
        noClasses()
            .that()
            .resideInAnyPackage("..application..", "..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .allowEmptyShould(true)
            .check(classes)
    }

    @Test
    fun `no domain type is annotated with persistence mapping`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .beAnnotatedWith("org.springframework.data.relational.core.mapping.Table")
            .orShould()
            .beAnnotatedWith("jakarta.persistence.Entity")
            .allowEmptyShould(true)
            .check(classes)
    }

    @Test
    fun `write modulith documentation into docs`() {
        Documenter(
            modules,
            Documenter.Options.defaults().withOutputFolder("docs/modulith"),
        ).writeDocumentation()
    }
}
