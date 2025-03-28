buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.6")
    }
}

plugins {
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}
