import com.diffplug.spotless.LineEnding

plugins {
    id("com.diffplug.spotless") version "8.8.0"
    java
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

modelmaker {
    features {
        jackson { enabled = true }
        validation { enabled = true }
        openApi { enabled = true }
    }
    src {
        enabled = true
    }
}

dependencies {
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.hibernate.validator:hibernate-validator:9.1.3.Final")

    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.30")

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
