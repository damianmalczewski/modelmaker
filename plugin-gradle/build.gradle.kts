import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("internal.java-common-convention")
    id("internal.jacoco-convention")
    id("internal.publishing-convention")
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.plugin.compatibility)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin {
    compilerOptions {
        explicitApi()

        javaParameters = true
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
    }
}
tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":modelmaker"))
    implementation(libs.gson) {
        libs.errorprone.annotations.get().let { exclude(group = it.group, module = it.name) }
    }

    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.assertj.bom))

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)

    testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
    website = "https://github.com/damianmalczewski/modelmaker"
    vcsUrl = "https://github.com/damianmalczewski/modelmaker.git"
    plugins {
        create("modelmaker") {
            id = "io.github.malczuuu.modelmaker"
            implementationClass = "io.github.malczuuu.modelmaker.gradle.ModelMakerPlugin"
            displayName = "ModelMaker Gradle Plugin"
            description = "Generates immutable Java model classes from schema files."
            tags =
                listOf(
                    "code-generation",
                    "codegen",
                    "java",
                    "dto",
                    "immutable",
                    "json",
                    "jackson",
                    "bean-validation",
                    "kotlin",
                )
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

internalPublishing {
    displayName = "ModelMaker Gradle Plugin"
    description = "Generates immutable Java model classes from schema files."
}
