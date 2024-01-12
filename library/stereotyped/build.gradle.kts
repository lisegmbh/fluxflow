dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.21")
    implementation(kotlin("reflect"))

    api(project(":api"))
    api(project(":reflection"))
}