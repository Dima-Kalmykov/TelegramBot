import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
}

group = "me.dmkal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.0")
    implementation(group="org.postgresql", name="postgresql", version="42.1.4")
    implementation("com.mashape.unirest:unirest-java:1.4.9")
}

tasks.test {
    useJUnit()
}



tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}