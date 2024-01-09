dependencies {
    implementation(kotlin("reflect"))
    
    implementation(project(":api"))
    implementation(project(":stereotyped"))

    api("jakarta.validation:jakarta.validation-api:3.0.2")
}