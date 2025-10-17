plugins {
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "com.toxicstoxm"
version = "3.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2")
    annotationProcessor("org.jetbrains:annotations:26.0.2")

    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("com.toxicstoxm:StormYAML:1.0.0")

    implementation("io.github.classgraph:classgraph:4.8.181")

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

tasks.test {
    useJUnitPlatform()
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

tasks.withType<JavaCompile>().configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
}

tasks.named<JavaCompile>("compileJava") {
    doFirst {
        val modulePath = configurations.compileClasspath.get().filter { it.name.endsWith(".jar") }

        options.compilerArgs = listOf(
            "--module-path", modulePath.joinToString(File.pathSeparator)
        )

        classpath = files()
    }
}