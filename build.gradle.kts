plugins {
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "com.toxicstoxm"
version = "3.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

val lombokVersion = "1.18.42"
val jetbrainsAnnotationsVersion = "26.0.2-1"
val junitVersion = "6.0.2"
val classgraphVersion = "4.8.184"
val stormYAMLVersion = "1.0.0"

dependencies {
    implementation("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    annotationProcessor("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")

    implementation("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    implementation("com.toxicstoxm:StormYAML:$stormYAMLVersion")

    implementation("io.github.classgraph:classgraph:$classgraphVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")

    testImplementation("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    testAnnotationProcessor("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
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