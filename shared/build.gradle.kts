plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvm()

    android {
        namespace = "com.weclimb.shared"
        compileSdk = 36
        minSdk = 23
        withHostTestBuilder {}.configure {}
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
