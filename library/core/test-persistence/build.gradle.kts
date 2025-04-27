dependencies {
    api(project(":core:persistence"))
    api(project(":core:migration"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.19.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
}