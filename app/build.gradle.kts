plugins { id("com.android.application") }
android {
    namespace = "com.example.recipebook"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.recipebook"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.core:core:1.16.0")
}
