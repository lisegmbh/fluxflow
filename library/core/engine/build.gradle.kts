dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
    
    implementation(project(":core:api"))
    implementation(project(":core:persistence"))
    implementation(project(":core:scheduling"))
    implementation(project(":core:reflection"))
    implementation(project(":core:stereotyped"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":springboot:springboot-testing"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}