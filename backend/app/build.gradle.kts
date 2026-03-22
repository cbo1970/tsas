plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common-module"))
    implementation(project(":player-module"))
    implementation(project(":match-module"))
    implementation(project(":statistics-module"))
    implementation(project(":auth-module"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.security:spring-security-test")
}
