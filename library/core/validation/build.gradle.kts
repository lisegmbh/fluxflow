dependencies {
    implementation(kotlin("reflect"))
    
    implementation(project(":core:api"))
    implementation(project(":core:stereotyped"))
    implementation(project(":core:reflection"))
    
    api("jakarta.validation:jakarta.validation-api:3.1.0")
    
    // Test
    testImplementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    testImplementation("jakarta.el:jakarta.el-api:6.0.0")
}