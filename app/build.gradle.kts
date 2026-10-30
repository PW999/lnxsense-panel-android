plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

compilerOptions {
	jvmTarget.set(JvmTarget.JVM_17)
} 


android {
    namespace = "com.pw999.lnxsense.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pw999.lnxsense.android"
        // API 34 (Android 14) is the minimum for native Java record support.
        // The server sends Group[] and Reading[] via Java ObjectOutputStream, and
        // deserialization requires the record class definitions to match exactly.
        // On API < 34, D8 desugars records into regular classes which may produce
        // a different serialVersionUID, breaking ObjectInputStream compatibility.
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 17 for the API record sources under java/com/pw999/lnxsense/api/.
        // These are copies of the server's API module (originally compiled with Java 25)
        // but stripped of JPMS module-info and Java 23+ markdown doc comments (///).
        // They must stay in the same package with identical field names/types/order
        // to preserve Java serialization compatibility with the server.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.class")
    }
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.class")
    }
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("**/*.exec") })
}

dependencies {
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle & ViewModel
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    // lifecycle-process provides ProcessLifecycleOwner, used to disconnect
    // the socket when the app goes to background (saves battery)
    implementation(libs.lifecycle.process)

    // Activity
    implementation(libs.activity.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // Serialization — used for persisting DialConfig to DataStore as JSON
    implementation(libs.serialization.json)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
