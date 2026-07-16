plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.assertj:assertj-core:3.27.7")

    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
