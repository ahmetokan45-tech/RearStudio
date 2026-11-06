plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "de.rearstudio"
    compileSdk = 36
    defaultConfig { applicationId = "de.rearstudio"; minSdk = 35; targetSdk = 36; versionCode = 10; versionName = "0.10-preview" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.palette:palette:1.0.0")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.test.ext:junit-ktx:1.3.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
