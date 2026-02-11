plugins {
    id("java-library")
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "com.toxicstoxm"
version = "3.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.jetbrains.annotations)
    annotationProcessor(libs.jetbrains.annotations)

    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.stormyaml)
    implementation(libs.classgraph)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.jetbrains.annotations)
    testAnnotationProcessor(libs.jetbrains.annotations)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates("com.toxicstoxm", "YAJSI", version as String?)

    pom {
        name = "YAJSI"
        description = "YAJSI (Yet another Java settings implementation) is a high level YAML config file manager"
        inceptionYear = "2024"
        url = "https://github.com/ToxicStoxm/YAJSI/"
        licenses {
            license {
                name = "The GNU General Public License, Version 3.0"
                url = "https://www.gnu.org/licenses/gpl-3.0.html"
                distribution = "https://www.gnu.org/licenses/gpl-3.0.html"
            }
        }
        developers {
            developer {
                id = "toxicstoxm"
                name = "ToxicStoxm"
                url = "https://toxicstoxm.com"
            }
        }
        scm {
            url = "https://github.com/ToxicStoxm/YAJSI/"
            connection = "scm:git:git://github.com/ToxicStoxm/YAJSI.git"
            developerConnection = "scm:git:ssh://git@github.com/ToxicStoxm/YAJSI.git"
        }
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Automatic-Module-Name" to "YAJSI"
        )
    }
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
}