import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "eu.theblob42.idea.whichkey"
version = "0.11.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit", "junit", "4.12")

    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        pluginVerifier()
        plugins("IdeaVIM:2.24.0")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // Let the Gradle plugin set the since-build version. It defaults to the version of the IDE we're building against
            // specified as two components, `{branch}.{build}` (e.g., "241.15989"). There is no third component specified.
            // The until-build version defaults to `{branch}.*`, but we want to support _all_ future versions, so we set it
            // with a null provider (the provider is important).
            // By letting the Gradle plugin handle this, the Plugin DevKit IntelliJ plugin cannot help us with the "Usage of
            // IntelliJ API not available in older IDEs" inspection. However, since our since-build is the version we compile
            // against, we can never get an API that's newer - it would be an unresolved symbol.
            untilBuild.set(provider { null })
        }
    }

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
