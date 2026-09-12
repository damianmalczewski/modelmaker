import internal.InternalBuildExtension
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Task
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("maven-publish")
    id("net.ltgt.errorprone")
}

val internalBuild = extensions.create("internalBuild", InternalBuildExtension::class.java)
internalBuild.kover.convention(false)

private val javaToolchainVersion = 25
private val javaTargetVersion = 17

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchainVersion)
    }
    withSourcesJar()
    withJavadocJar()
}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
    options.errorprone {
        if (isTestTask()) {
            // Static analysis targets the published main sources; keep tests unconstrained.
            disableAllChecks = true
        } else {
            error("NullAway")
            option("NullAway:OnlyNullMarked", "true")
            option("NullAway:JSpecifyMode", "true")
        }
    }
}
tasks.named<JavaCompile>("compileJava").configure {
    options.release = javaTargetVersion
}

extensions.configure<KotlinJvmProjectExtension> {
    compilerOptions {
        jvmToolchain {
            languageVersion = JavaLanguageVersion.of(javaToolchainVersion)
        }
        explicitApi()
        moduleName = project.name
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
    }
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        javaParameters = true
    }
}
tasks.named<KotlinCompile>("compileKotlin").configure {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaTargetVersion.toString())
    }
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
        attributes["Build-Jdk-Spec"] = javaTargetVersion
        attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
    }
    from("${rootProject.rootDir}/LICENSE") {
        into("META-INF/")
        rename { "LICENSE.txt" }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = internalBuild.displayName
            description = internalBuild.description
            url = "https://github.com/damianmalczewski/modelmaker"
            inceptionYear = "2026"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "damianmalczewski"
                    name = "Damian Malczewski"
                    url = "https://github.com/damianmalczewski"
                }
            }
            scm {
                connection = "scm:git:git://github.com/damianmalczewski/modelmaker.git"
                developerConnection = "scm:git:ssh://github.com/damianmalczewski/modelmaker.git"
                url = "https://github.com/damianmalczewski/modelmaker"
            }
            issueManagement {
                system = "GitHub Issues"
                url = "https://github.com/damianmalczewski/modelmaker/issues"
            }
        }
    }
}

afterEvaluate {
    if (internalBuild.kover.get()) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        tasks.named("check") {
            finalizedBy(tasks.named("koverHtmlReport"), tasks.named("koverXmlReport"))
        }
    }
}

/**
 * A task is treated as a test task when its name contains "test" (case-insensitive) - used to relax
 * static-analysis checks on test compilation.
 */
fun Task.isTestTask(): Boolean = name.matches(Regex(".*[tT]est.*"))
