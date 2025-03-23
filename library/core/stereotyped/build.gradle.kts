dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")
    implementation(kotlin("reflect"))

    implementation(project(":core:api"))
    implementation(project(":core:reflection"))
}