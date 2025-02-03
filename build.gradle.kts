plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.josiwhitlock"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}