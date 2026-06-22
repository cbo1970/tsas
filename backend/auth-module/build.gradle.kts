plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    testFixturesImplementation("org.springframework.security:spring-security-test")
}
