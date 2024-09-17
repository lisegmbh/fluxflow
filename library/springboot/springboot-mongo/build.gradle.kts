dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.20")

    implementation("org.springframework.data:spring-data-mongodb")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.mongodb:mongodb-driver-sync:5.1.4")


    implementation(project(":core:api"))
    implementation(project(":core:persistence"))
    implementation(project(":core:reflection"))
    implementation(project(":core:migration"))
}