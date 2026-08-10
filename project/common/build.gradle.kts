plugins {
    `java-library`
}

dependencies {
    api("org.springframework:spring-web")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("org.springframework.security:spring-security-oauth2-jose")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    api("org.springframework.security:spring-security-oauth2-resource-server")
    api("io.projectreactor:reactor-core")
    api("org.slf4j:slf4j-api")
    api("io.projectreactor.kafka:reactor-kafka")
}
