dependencies {
    implementation(kotlin("reflect"))
    
    implementation(project(":core:api"))
    implementation(project(":core:stereotyped"))
    implementation(project(":core:reflection"))
    
    api("jakarta.validation:jakarta.validation-api:3.0.2")
}