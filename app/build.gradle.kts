plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.ziyuan.immersive"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ziyuan.immersive"
        minSdk = 24
        targetSdk = 36
        versionCode = 37
        versionName = "1.37"
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
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(files("libs/apksig.jar"))
    implementation("org.apache.commons:commons-compress:1.26.0")

    implementation(files("libs/pinyin4j-2.5.0.jar"))
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
    implementation(files("libs/apksig.jar"))
    implementation(files("libs/pinyin4j-2.5.0.jar"))
}
