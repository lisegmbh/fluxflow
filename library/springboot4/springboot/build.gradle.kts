val apiConsumer = sourceSets.create("apiConsumer") {
    java.srcDir("../../springboot/springboot/src/apiConsumer/java")
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("../../springboot/springboot/src/main/kotlin")
            resources.srcDir("../../springboot/springboot/src/main/resources")
        }
        test {
            kotlin.srcDir("../../springboot/springboot/src/testShared/kotlin")
        }
    }
}

sourceSets {
    test {
        java.srcDir("../../springboot/springboot/src/testShared/java")
    }
}

dependencies {
    api(project(":core:api"))
    api(project(":core:engine"))
    api(project(":core:stereotyped"))

    implementation(kotlin("reflect"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    api(project(":core:reflection"))
    implementation(project(":core:persistence"))
    implementation(project(":core:scheduling"))
    implementation(project(":core:validation"))
    implementation(project(":core:migration"))

    runtimeOnly(project(":springboot4:springboot-test-scheduling"))

    testImplementation(project(":springboot4:springboot-testing"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    add(apiConsumer.implementationConfigurationName, project(":springboot4:springboot"))
}

tasks.named("check") {
    dependsOn(apiConsumer.classesTaskName)
}
