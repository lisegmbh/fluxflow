dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    implementation(kotlin("reflect"))

    implementation(project(":core:api"))
    implementation(project(":core:reflection"))
}