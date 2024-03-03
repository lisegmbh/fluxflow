dependencies {
    implementation(kotlin("reflect"))
    
    implementation(project(":core:api"))
    implementation(project(":core:stereotyped"))

    api("jakarta.validation:jakarta.validation-api:3.0.2")
}