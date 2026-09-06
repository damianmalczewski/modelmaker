plugins {
    `java-library`
    id("internal.errorprone-convention")
    id("internal.jacoco-convention")
    id("internal.java-common-convention")
    id("internal.publishing-convention")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(libs.gson)

    compileOnly(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.assertj.bom))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.gson)

    testRuntimeOnly(libs.junit.platform.launcher)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

internalPublishing {
    displayName = "ModelMaker Core"
    description = "Schema parsing and source emitters shared by the ModelMaker plugins."
}
