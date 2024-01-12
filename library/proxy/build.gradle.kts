dependencies {
    api(project(":api"))
    api(project(":stereotyped"))
    implementation(kotlin("reflect"))
    implementation("net.bytebuddy:byte-buddy:1.14.11")
}