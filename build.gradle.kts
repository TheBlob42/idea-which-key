import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.20"
    id("org.jetbrains.intellij") version "1.8.0"
}

group = "eu.theblob42.idea.whichkey"
version = "0.9.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    testImplementation("junit", "junit", "4.12")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<PatchPluginXmlTask> {
    sinceBuild.set("222")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2022.2")
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("IdeaVIM:1.11.1"))
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {

}
