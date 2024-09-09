dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.20")
    implementation(kotlin("reflect"))

    implementation(project(":core:api"))
    implementation(project(":core:reflection"))
}