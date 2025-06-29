dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.0")
    implementation(kotlin("reflect"))

    implementation(project(":core:api"))
    implementation(project(":core:reflection"))
}