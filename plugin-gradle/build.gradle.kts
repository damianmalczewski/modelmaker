import org.gradle.plugin.compatibility.compatibility

plugins {
    `kotlin-dsl`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.plugin.compatibility)

    id("internal.build-convention")
}

internalBuild {
    displayName = "ModelMaker Gradle Plugin"
    description = "Gradle plugin of the ModelMaker project, generating Java model classes from schema files."
    kover = false
}

dependencies {
    implementation(project(":modelmaker"))

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
            description =
                "Wires generation of Java model classes with various annotation support from schema files into compilation process."
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

tasks.withType<Jar>().configureEach {
    if (name == "javadocJar") {
        enabled = false
    }
}
