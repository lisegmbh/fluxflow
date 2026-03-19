plugins {
    antlr
}

dependencies {
    implementation(project(":core:api"))
    implementation(project(":core:reflection"))
    implementation(project(":core:rest"))

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("jakarta.servlet:jakarta.servlet-api")

    implementation(kotlin("reflect"))

    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf(
        "-visitor",
        "-long-messages",
        "-package",
        "de.lise.fluxflow.springboot.odata.filter.grammar"
    )


    val antlrPackage = "de.lise.fluxflow.springboot.odata.filter.grammar"
    val antlrPackagePath = antlrPackage.replace('.', '/')

    outputDirectory = layout.buildDirectory.dir("generated-src/antlr/main/$antlrPackagePath").get().asFile
}