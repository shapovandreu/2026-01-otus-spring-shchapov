plugins {
    java
    id("org.springframework.boot") version "3.5.8" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of((property("javaVersion") as String).toInt())
        }
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        }
    }

    dependencies {
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("io.projectreactor:reactor-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    plugins.withId("org.springframework.boot") {
        tasks.named<Jar>("jar") {
            enabled = false
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    plugins.withId("java") {
        val testSourceSet = extensions.getByType<SourceSetContainer>()["test"]
        tasks.named<Test>("test") {
            useJUnitPlatform {
                excludeTags("integration")
            }
        }
        tasks.register<Test>("integrationTest") {
            description = "Интеграционные тесты на Testcontainers (нужен запущенный Docker)"
            group = "verification"
            testClassesDirs = testSourceSet.output.classesDirs
            classpath = testSourceSet.runtimeClasspath
            useJUnitPlatform {
                includeTags("integration")
            }
            filter { isFailOnNoMatchingTests = false }
            shouldRunAfter(tasks.named("test"))
        }
    }
}
