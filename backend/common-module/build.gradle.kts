dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    // DataIntegrityViolationException + AccessDeniedException leben in spring-tx
    // bzw. spring-security-core. Beide werden vom globalen ExceptionHandler hier
    // gefangen (TEN-61), damit nicht jedes Modul eigene Advice-Klassen braucht.
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
}
