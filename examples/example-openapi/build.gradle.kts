plugins {
    java
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

modelmaker {
    src {
        enabled = true
    }
    features {
        jackson { enabled = true }
        validation { enabled = true }
        openApi { enabled = true }
    }
}

dependencies {
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.4"))
    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.hibernate.validator:hibernate-validator:9.1.6.Final")

    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.55")

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
