dependencies {
    implementation("org.springframework:spring-context:6.0.13")
    implementation(project(":springboot"))
    
    api(project(":test-persistence"))
    api(project(":test-scheduling"))
}