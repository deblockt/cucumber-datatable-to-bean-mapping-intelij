import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
    id("me.qoomon.git-versioning") version "6.3.6"
}

group = "io.github.deblockt"
version = ""
gitVersioning.apply {
    refs {
        branch(".+") {
            version = "\${ref}-SNAPSHOT"
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    // optional fallback configuration in case of no matching ref configuration
    rev {
        version = "\${commit}"
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("io.github.deblockt:cucumber-datatable-to-bean-mapping:1.1.0")
    implementation("io.cucumber:cucumber-core:7.8.1")
    implementation("org.apache.commons:commons-text:1.12.0")

    intellijPlatform {
        intellijIdea("2025.3")

        bundledPlugin("com.intellij.java")
        plugin("gherkin", "253.28294.218")
        plugin("cucumber-java", "253.28294.251")
    }
}

intellijPlatform {
    pluginVerification {
        ides {
            val verifyIdeVersion = providers.gradleProperty("verifyIdeVersion").orNull
            if (verifyIdeVersion != null) {
                ide(IntelliJPlatformType.IntellijIdeaCommunity, verifyIdeVersion)
            } else {
                ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
                recommended()
            }
        }
    }
}


kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("251.27812")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}