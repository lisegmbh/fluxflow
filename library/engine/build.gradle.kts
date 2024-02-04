dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    
    implementation(project(":api"))
    implementation(project(":persistence"))
    implementation(project(":scheduling"))
    implementation(project(":reflection"))
    implementation(project(":stereotyped"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":testing"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}