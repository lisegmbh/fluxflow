dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")

    implementation("org.springframework.data:spring-data-mongodb")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.mongodb:mongodb-driver-sync:5.0.0")


    implementation(project(":core:api"))
    implementation(project(":core:persistence"))
    implementation(project(":core:reflection"))
}