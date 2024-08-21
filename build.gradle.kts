import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "eu.theblob42.idea.whichkey"
version = "0.10.3-test"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    testImplementation("junit", "junit", "4.12")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<PatchPluginXmlTask> {
    sinceBuild.set("233")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2023.3.3")
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("IdeaVIM:2.10.0"))
}
