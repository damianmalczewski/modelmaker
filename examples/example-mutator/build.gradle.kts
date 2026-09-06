import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless") version "8.8.0"
    kotlin("jvm") version "2.4.10"
    id("io.github.malczuuu.modelmaker") version "1.0.0-SNAPSHOT"
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
    features {
        jackson = false
        validation = false
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

spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("build/**", "src/main/model/kotlin/**")

        ktfmt().metaStyle().configure {
            it.setMaxWidth(100)
            it.setRemoveUnusedImports(true)
        }
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    java {
        target("src/**/*.java")
        targetExclude("build/**", "src/main/model/java/**")

        googleJavaFormat()
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    kotlinGradle {
        target("*.gradle.kts")
        targetExclude("build/**")

        ktlint().editorConfigOverride(
            mapOf("max_line_length" to "120"),
        )
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
}

defaultTasks("spotlessApply", "build")
