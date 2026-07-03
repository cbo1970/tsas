import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    base
    jacoco
}

allprojects {
    group = "com.cas.tsas"
    version = "0.1.0"
}

repositories {
    // Needed so the root project can resolve the JaCoCo Ant artifacts used by
    // the aggregate report / verification tasks defined below.
    mavenCentral()
}

jacoco {
    toolVersion = "0.8.14"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
        }
    }

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.14"
    }

    tasks.withType<JavaCompile> {
        options.release = 25
        options.compilerArgs.add("-parameters")
    }

    repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.spring.io/milestone")
            mavenContent { releasesOnly() }
        }
    }

    dependencies {
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.testcontainers:testcontainers-junit-jupiter")
        "testImplementation"("org.testcontainers:testcontainers-postgresql")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Per-module report (run on demand: `./gradlew :match-module:jacocoTestReport`).
    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            csv.required.set(true)
            html.required.set(true)
        }
    }
}

// --- Aggregate coverage report + gate across all modules ---------------------
// Integration tests (`*IT`) live in :app but exercise classes across every
// module. A per-module gate would therefore mis-report (e.g. controllers in
// match-module look "uncovered" because their exec data lives in :app). We
// aggregate every module's test.exec against all main sources and enforce the
// threshold on the combined result.
//
//   ./gradlew jacocoRootReport               -> HTML/XML/CSV at build/reports/jacoco/jacocoRootReport
//   ./gradlew jacocoRootCoverageVerification -> the gate (also wired into `check`)
gradle.projectsEvaluated {
    val javaSubs = subprojects.filter { it.plugins.hasPlugin("java") }
    val mainSourceSets = javaSubs.map {
        it.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    }
    val testTasks = javaSubs.map { "${it.path}:test" }

    // Resolved lazily at execution time, after the test tasks have produced
    // their exec files (modules without tests simply have none).
    val execData = provider {
        javaSubs
            .map { it.layout.buildDirectory.file("jacoco/test.exec").get().asFile }
            .filter { it.exists() }
    }
    val classDirs = files(mainSourceSets.map { it.output })
    val sourceDirs = files(mainSourceSets.flatMap { it.allSource.srcDirs })

    tasks.register<JacocoReport>("jacocoRootReport") {
        group = "verification"
        description = "Aggregated JaCoCo coverage report across all modules."
        dependsOn(testTasks)
        executionData.setFrom(execData)
        classDirectories.setFrom(classDirs)
        sourceDirectories.setFrom(sourceDirs)
        reports {
            xml.required.set(true)
            csv.required.set(true)
            html.required.set(true)
        }
    }

    val coverageGate = tasks.register<JacocoCoverageVerification>("jacocoRootCoverageVerification") {
        group = "verification"
        description = "Fails the build if aggregated coverage drops below the threshold."
        dependsOn(testTasks)
        executionData.setFrom(execData)
        classDirectories.setFrom(classDirs)
        sourceDirectories.setFrom(sourceDirs)
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.85".toBigDecimal()
                }
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
            }
        }
    }

    // The gate becomes part of the standard `check` lifecycle.
    tasks.named("check") {
        dependsOn(coverageGate)
    }
}
