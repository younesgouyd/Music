group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.jetbrains)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(libs.coroutines.core)

    implementation(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material") // todo
    }
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.sqldelight.sqliteDriver)
    implementation(libs.sqldelight.jdbcDriver)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.json)
    implementation(libs.vlcj)
    implementation("com.mpatric:mp3agic:0.9.1")
}

sqldelight {
    databases {
        create("YounesMusic") {
            deriveSchemaFromMigrations.set(true)
            dialect(libs.sqldelight.sqliteDialect)
            packageName.set("dev.younesgouyd.apps.music.app.data.sqldelight")
        }
    }
}