plugins {
    id("org.springframework.boot")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir.parentFile
}

// Expose the backend root so ArchitectureTest can import each module's compiled classes
// directly (Gradle's test classpath is delivered via a pathing jar that ArchUnit's
// classpath scanner cannot read).
tasks.named<Test>("test") {
    systemProperty("tsas.rootDir", rootProject.projectDir.absolutePath)
}

dependencies {
    implementation(project(":common-module"))
    implementation(project(":player-module"))
    implementation(project(":match-module"))
    implementation(project(":statistics-module"))
    implementation(project(":auth-module"))
    implementation(project(":ai-module"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-jackson2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(testFixtures(project(":auth-module")))
}
