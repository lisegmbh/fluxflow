dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.21")

    implementation("org.springframework.data:spring-data-mongodb:4.1.5")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.1.5")

    implementation("org.mongodb:mongodb-driver-sync:4.11.1")


    implementation(project(":api"))
    implementation(project(":persistence"))
    implementation(project(":reflection"))
}