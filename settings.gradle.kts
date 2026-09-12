pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "modelmaker-build"

include(":modelmaker")
include(":plugin-gradle")
include(":plugin-maven")

project(":plugin-gradle").name = "modelmaker-gradle-plugin"
project(":plugin-maven").name = "modelmaker-maven-plugin"
