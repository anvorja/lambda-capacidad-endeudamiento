package com.crediya.capacidad.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * La regla de dependencia: el dominio no conoce nada de afuera; la aplicacion solo conoce el dominio.
 */
@AnalyzeClasses(packages = "com.crediya.capacidad", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_aplicacion_ni_de_infraestructura = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule el_dominio_no_usa_aws_json_ni_logs = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "software.amazon..", "com.amazonaws..", "com.fasterxml..", "org.slf4j..", "java.util.logging..");

    @ArchTest
    static final ArchRule la_aplicacion_no_depende_de_infraestructura = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "software.amazon..", "com.amazonaws..");
}
