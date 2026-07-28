import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val versionPropertiesFile = rootProject.file("version.properties")

fun loadVersionProperties(): Properties = Properties().apply {
    if (versionPropertiesFile.exists()) {
        versionPropertiesFile.inputStream().use { load(it) }
    }
}

fun readVersionInt(props: Properties, key: String, default: Int): Int =
    props.getProperty(key)?.toIntOrNull() ?: default

fun versionNameFrom(props: Properties): String {
    val major = readVersionInt(props, "versionMajor", 1)
    val minor = readVersionInt(props, "versionMinor", 0)
    val patch = readVersionInt(props, "versionPatch", 0)
    return "$major.$minor.$patch"
}

val versionProperties = loadVersionProperties()
val appVersionMajor = readVersionInt(versionProperties, "versionMajor", 1)
val appVersionMinor = readVersionInt(versionProperties, "versionMinor", 0)
val appVersionPatch = readVersionInt(versionProperties, "versionPatch", 0)
val appVersionCode = readVersionInt(versionProperties, "versionCode", 1)
val appVersionName = versionNameFrom(versionProperties)

android {
    namespace = "com.learning.tasktracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.learning.tasktracker"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("int", "VERSION_CODE", "$appVersionCode")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

fun writeVersionProperties(versionCode: Int) {
    val props = loadVersionProperties()
    props.setProperty("versionMajor", appVersionMajor.toString())
    props.setProperty("versionMinor", appVersionMinor.toString())
    props.setProperty("versionPatch", appVersionPatch.toString())
    props.setProperty("versionCode", versionCode.toString())
    versionPropertiesFile.outputStream().use { output ->
        props.store(output, "Application version for APK builds")
    }
}

tasks.register("bumpVersionCode") {
    group = "versioning"
    description = "Increment versionCode in version.properties"
    doLast {
        val props = loadVersionProperties()
        val currentCode = readVersionInt(props, "versionCode", 1)
        val nextCode = currentCode + 1
        writeVersionProperties(nextCode)
        logger.lifecycle("versionCode -> $nextCode (versionName stays $appVersionName)")
        logger.lifecycle("Run assembleDebug or publishDebugApk to build the new version.")
    }
}

tasks.register("publishDebugApk") {
    group = "publishing"
    description = "Build debug APK and copy versioned artifact to releases/"
    dependsOn("assembleDebug")

    doLast {
        val props = loadVersionProperties()
        val versionName = versionNameFrom(props)
        val versionCode = readVersionInt(props, "versionCode", 1)
        val versionedApkName = "task-tracker-$versionName-$versionCode-debug.apk"

        val debugDir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        val source = debugDir.listFiles()
            ?.firstOrNull { it.isFile && it.name.endsWith(".apk") }
            ?: error("APK not found in ${debugDir.absolutePath}")

        val releases = rootProject.file("releases").apply { mkdirs() }
        val versionedTarget = releases.resolve(versionedApkName)
        source.copyTo(versionedTarget, overwrite = true)
        source.copyTo(releases.resolve("task-tracker-debug.apk"), overwrite = true)

        logger.lifecycle("Published ${versionedTarget.name}")
        logger.lifecycle("Updated releases/task-tracker-debug.apk")
    }
}
