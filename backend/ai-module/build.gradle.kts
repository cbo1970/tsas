dependencies {
    implementation(project(":common-module"))
    implementation(project(":match-module"))
    implementation(project(":player-module"))
    implementation(project(":statistics-module"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M6"))
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    testImplementation("org.wiremock:wiremock-standalone:3.10.0")
}
