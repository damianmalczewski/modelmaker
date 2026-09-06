import internal.isTestTask
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java")
    id("net.ltgt.errorprone")
}

tasks.withType<JavaCompile>().configureEach {
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
