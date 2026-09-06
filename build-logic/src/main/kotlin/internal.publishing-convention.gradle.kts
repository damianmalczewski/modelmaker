import internal.InternalPublishingExtension

plugins {
    id("maven-publish")
}

val internalPublishing = extensions.create("internalPublishing", InternalPublishingExtension::class.java)

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = internalPublishing.displayName
            description = internalPublishing.description
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
