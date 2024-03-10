dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
    implementation(kotlin("reflect"))

    implementation(project(":core:api"))
    implementation(project(":core:reflection"))
}