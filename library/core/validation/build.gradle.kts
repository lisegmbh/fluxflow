dependencies {
    implementation(kotlin("reflect"))
    
    implementation(project(":core:api"))
    implementation(project(":core:stereotyped"))
    implementation(project(":core:reflection"))
    
    api("jakarta.validation:jakarta.validation-api:3.1.1")
    
    // Test
    testImplementation("org.hibernate.validator:hibernate-validator:8.0.2.Final")
    testImplementation("jakarta.el:jakarta.el-api:6.0.1")
}