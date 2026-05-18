dependencies {
    implementation(kotlin("reflect"))
    
    implementation(project(":core:api"))
    implementation(project(":core:stereotyped"))
    implementation(project(":core:reflection"))
    
    api("jakarta.validation:jakarta.validation-api")
    
    // Test
    testImplementation("org.hibernate.validator:hibernate-validator")
    // Not in spring-boot-dependencies; explicit version required for EL API in unit tests.
    testImplementation("jakarta.el:jakarta.el-api:6.0.1")
}