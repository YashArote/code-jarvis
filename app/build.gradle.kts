

plugins {
    id("com.android.application") version "8.2.1"

}

android {
    namespace = "com.example.code_jarvis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.code_jarvis"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies{

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.android.support.constraint:constraint-layout:2.0.4")

    implementation(platform("io.github.Rosemoe.sora-editor:bom:0.23.5"))
    implementation("io.github.Rosemoe.sora-editor:editor")
    implementation("io.github.Rosemoe.sora-editor:language-textmate")
    implementation("org.json:json:20090211")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("ch.usi.si.seart:java-tree-sitter:1.12.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-mobile:1.17.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.amitshekhariitbhu:PRDownloader:1.0.2")
    implementation("androidx.work:work-runtime:2.10.1") // Or latest
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}