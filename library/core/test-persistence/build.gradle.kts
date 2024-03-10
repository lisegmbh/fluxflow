dependencies {
    api(project(":core:persistence"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
}