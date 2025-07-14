import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "com.toxicstoxm"
version = "2.1.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2")
    annotationProcessor("org.jetbrains:annotations:26.0.2")

    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("org.yaml:snakeyaml:2.4")

    implementation("io.github.classgraph:classgraph:4.8.179")

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.toxicstoxm.YAJSI.YAJSISettingsManager"
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates("com.toxicstoxm.YAJSI", "YAJSI", version as String?)

    pom {
        name = "YAJSI"
        description = "YAJSI (Yet another Java settings implementation) uses SnakeYAML and some cool logic to make your life easier"
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
                url = "https://github.com/ToxicStoxm/"
            }
        }
        scm {
            url = "https://github.com/ToxicStoxm/YAJSI/"
            connection = "scm:git:git://github.com/ToxicStoxm/YAJSI.git"
            developerConnection = "scm:git:ssh://git@github.com/ToxicStoxm/YAJSI.git"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
}