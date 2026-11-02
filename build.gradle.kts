import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

fun prop(name: String): String = providers.gradleProperty(name).get()

// Optional, machine-specific settings in a git-ignored `local.properties`, e.g.
//   platformLocalPath=/Applications/PyCharm.app
// A -PplatformLocalPath=... flag or ~/.gradle/gradle.properties entry works too and takes precedence.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.isFile) f.inputStream().use(::load)
}
val platformLocalPath: String? =
    providers.gradleProperty("platformLocalPath").orNull ?: localProperties.getProperty("platformLocalPath")

group = prop("pluginGroup")
version = prop("pluginVersion")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Build against a locally installed PyCharm if one is configured, otherwise download platformVersion.
        val localPath = platformLocalPath?.takeIf { it.isNotBlank() }
        if (localPath != null) {
            require(file(localPath).exists()) { "platformLocalPath does not exist: $localPath" }
            local(localPath)
        } else {
            create(IntelliJPlatformType.PyCharm, prop("platformVersion"))
        }
        bundledPlugin("PythonCore")
        plugin("com.redhat.devtools.lsp4ij", prop("lsp4ijVersion"))
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "dk.ingi.pyreflypycharm"
        name = prop("pluginName")
        version = prop("pluginVersion")
        ideaVersion {
            sinceBuild = prop("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
    // Not published to JetBrains Marketplace; releases are attached to GitHub.
    buildSearchableOptions = false
}

// `./gradlew runIde -PrunIdeArgs=/path/to/project,/path/to/project/file.py` opens a project (and files)
// in the sandbox IDE. The system properties skip first-run dialogs that would block an unattended start.
tasks.named<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runIde") {
    val runArgs = providers.gradleProperty("runIdeArgs").map { it.split(',').filter(String::isNotBlank) }.orElse(emptyList())
    argumentProviders += CommandLineArgumentProvider { runArgs.get() }
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf(
            "-Didea.trust.all.projects=true",
            "-Djb.consents.confirmation.enabled=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Dide.show.tips.on.startup.default.value=false",
        )
    }
}
