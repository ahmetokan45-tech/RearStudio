plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "de.rearstudio"
    compileSdk = 35
    defaultConfig { applicationId = "de.rearstudio"; minSdk = 30; targetSdk = 35; versionCode = 5; versionName = "0.5-preview" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    testImplementation("junit:junit:4.13.2")
}
