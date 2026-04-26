dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.21")

    implementation("org.springframework.data:spring-data-mongodb")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.mongodb:mongodb-driver-sync")


    implementation(project(":core:api"))
    implementation(project(":core:persistence"))
    implementation(project(":core:reflection"))
    implementation(project(":core:migration"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    testImplementation("org.testcontainers:testcontainers:2.0.4")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.4")
    testImplementation("org.testcontainers:testcontainers-mongodb:2.0.4")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}