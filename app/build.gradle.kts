plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.ziyuan.Assistant"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ziyuan.Assistant"
        minSdk = 24
        targetSdk = 36
        versionCode = 39
        versionName = "1.39"
    }
    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = "ziyuan2026"
            keyAlias = "ziyuan"
            keyPassword = "ziyuan2026"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // —— 以下为补全的排除项，专门解决“安装包名/META-INF 冲突” ——
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/*.version"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.md"
            excludes += "META-INF/versions/**"
            // 若重跑后仍报 “More than one file was found with OS independent path 'META-INF/xxx'”，
            // 把报错里的具体路径追加到这一排除清单即可。
        }
    }
}
dependencies {
    // —— 修复点1：本地 jar 每个只声明一次（原文件 apksig.jar、pinyin4j 各被写了两次）——
    implementation(files("libs/apksig.jar"))
    implementation(files("libs/sora-editor-classes.jar"))
    implementation(files("libs/pinyin4j-2.5.0.jar"))
    // 注意：libs/ 下还有 annotation-jvm.jar、collection-jvm.jar 未被引用。
    // 若确实需要请在此显式声明；否则建议从仓库删除，避免将来被 fileTree 误引入导致重复类。

    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("com.arthenica:smart-exception-java:0.2.1")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("commons-io:commons-io:2.11.0")
}
