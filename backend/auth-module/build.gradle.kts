plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    // AuditorAware lives in spring-data-commons. Pulled in (not the full JPA starter)
    // so the auth-module stays free of Hibernate/JPA dependencies.
    implementation("org.springframework.data:spring-data-commons")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    testFixturesImplementation("org.springframework.security:spring-security-test")
}
