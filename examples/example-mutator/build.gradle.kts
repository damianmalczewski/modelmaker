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
    features {
        jackson { enabled = false }
        validation { enabled = false }
    }
    kotlin {
        enabled = true
    }
}

dependencies {
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
