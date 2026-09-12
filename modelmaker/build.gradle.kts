plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)

    id("internal.build-convention")
}

internalBuild {
    displayName = "ModelMaker Core"
    description = "Shared core of the ModelMaker project, generating Java model classes from schema files."
    kover = true
}

dependencies {
    implementation(libs.gson)

    compileOnly(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.assertj.bom))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)

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
