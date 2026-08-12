import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinxCoroutinesCore)
        }

        val androidAndJvmMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.server.cors)
            }
        }

        androidMain {
            dependsOn(androidAndJvmMain)
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.androidx.core.ktx)
                api(libs.kthttp)
            }
        }
        jvmMain {
            dependsOn(androidAndJvmMain)
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutinesSwing)
                api(libs.kthttp)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.ohuang.kmp.filemanager.kmp_filemanager.sharedLogic"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}