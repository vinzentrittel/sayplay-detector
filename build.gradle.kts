import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("org.jetbrains.dokka").version("2.2.0")
}

android {
    namespace = "de.vinzentrittel.sayplay.detector"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {}
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.csv)
    implementation(libs.opencv.android.ximgproc)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}

tasks.withType<Test> {
    // Mocking library introduced an annoying compiler warning, this silences it for now
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "de.vinzentrittel.sayplay"
                artifactId = "detector"
                version = "0.0.5"
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/vinzentrittel/sayplay-detector")
                credentials {
                    username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                    password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
