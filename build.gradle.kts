import com.diffplug.spotless.LineEnding

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":modelmaker"))
}

spotless {
    java {
        target("**/src/**/*.java")
        targetExclude("examples/**", "**/build/**", "**/package-info.java", "**/module-info.java")

        licenseHeaderFile("${rootProject.rootDir}/gradle/license-header.txt")
        googleJavaFormat()
        formatAnnotations()
        forbidWildcardImports()
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
    format("javaMisc") {
        target("**/src/**/package-info.java", "**/src/**/module-info.java")
        targetExclude("examples/**", "**/build/**")

        // License headers in these files are not formatted with standard java group, so we need to use custom
        // settings. The regex is designed to find out where the code starts in these files, so the license
        // header can be placed before it.
        //
        // The code starts with either:
        //
        // - any annotation (ex. @NullMarked before package declaration),
        // - package, module or import declaration,
        // - "/**" in case of a pre-package (or pre-module) JavaDoc.
        val delimiter = "^(@|package|import|module|/\\*\\*)"

        licenseHeaderFile("${rootProject.rootDir}/gradle/license-header.txt", delimiter)
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
    kotlin {
        target(
            "**/src/**/*.kt",
            "**/src/**/*.kt",
        )
        targetExclude("examples/**", "**/build/**")

        licenseHeaderFile("${rootProject.rootDir}/gradle/license-header.txt")
        ktfmt().metaStyle().configure {
            it.setMaxWidth(100)
            it.setRemoveUnusedImports(true)
        }
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts", "build-logic/**/*.gradle.kts")
        targetExclude("**/build/**")

        ktlint().editorConfigOverride(
            mapOf("max_line_length" to "120"),
        )
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
    format("misc") {
        target(".gitattributes", ".gitignore")
        targetExclude("**/build/**")

        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
}

tasks.named("check") {
    finalizedBy(tasks.named("koverHtmlReport"), tasks.named("koverXmlReport"))
}

defaultTasks("spotlessApply", "build", "publishToMavenLocal")
