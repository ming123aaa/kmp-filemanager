import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":sharedUI"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.material3)
    implementation(libs.compose.uiToolingPreview)
}


compose.desktop {
    application {
        mainClass = "com.ohuang.kmp.filemanager.kmp_filemanager.MainKt"


        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "FileManager"
            packageVersion = "1.0.0"
            vendor="ming"

            macOS {
                iconFile.set(project.file("src/main/resources/icon_ico.ico"))
            }
            windows {
                shortcut = true
                menu = true
                perUserInstall = true
                dirChooser = true
                console = false
                msiPackageVersion = packageVersion
                exePackageVersion = packageVersion
                upgradeUuid="24f391d3-123a-4d25-b7b8-726a65975220"
                iconFile.set(project.file("src/main/resources/icon_ico.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon_ico.ico"))
            }
        }


    }
}