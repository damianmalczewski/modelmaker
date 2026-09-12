plugins {
    `java-library`
    alias(libs.plugins.maven.plugin.development)

    id("internal.build-convention")
}

internalBuild {
    displayName = "ModelMaker Maven Plugin"
    description = "Maven plugin of the ModelMaker project, generating Java model classes from schema files."
}

mavenPlugin {
    artifactId = "modelmaker-maven-plugin"
    name = "ModelMaker Maven Plugin"
    description = "Generates immutable Java model classes from schema files."
}

dependencies {
    implementation(project(":modelmaker"))

    // Provided by Maven itself at execution time, so they stay off the published POM.
    compileOnly(libs.jspecify)
    compileOnly(libs.maven.plugin.api)
    compileOnly(libs.maven.core)
    compileOnly(libs.maven.plugin.annotations)

    testImplementation(libs.maven.plugin.api)
    testImplementation(libs.maven.core)

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
