plugins {
    id("org.jetbrains.intellij") version "0.6.5"
    java
    kotlin("jvm") version "1.4.20"
}

group = "eu.theblob42.idea.whichkey"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.2")

    implementation("org.apache.logging.log4j", "log4j-core", "2.14.0")
    implementation("org.apache.logging.log4j", "log4j-api", "2.14.0")

    testImplementation("junit", "junit", "4.12")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.1"
    // do not patch plugin.xml since/until build with values inferred from the intellij version
    updateSinceUntilBuild = false

    setPlugins("IdeaVIM:0.62")
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}