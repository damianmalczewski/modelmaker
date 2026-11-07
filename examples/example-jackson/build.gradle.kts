import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    id("io.github.malczuuu.modelmaker") version "0.1.0"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
}
tasks.named<JavaCompile>("compileJava") {
    options.release = 17
}

kotlin {
    jvmToolchain(jdkVersion = 25)

    compilerOptions {
        javaParameters = true
        apiVersion = KotlinVersion.KOTLIN_2_4
        languageVersion = KotlinVersion.KOTLIN_2_4
    }
}
tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

modelmaker {
    src {
        enabled = true
    }
    schemas {
        directory = layout.projectDirectory.dir("src/main/schemas")
    }
    features {
        // Each file has an override for it.
        jackson { enabled = false }
        validation { enabled = false }
    }
    kotlin {
        enabled = true
    }
}

dependencies {
    implementation(platform("tools.jackson:jackson-bom:3.2.4"))
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(platform("org.assertj:assertj-bom:3.27.7"))
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

defaultTasks("build")
