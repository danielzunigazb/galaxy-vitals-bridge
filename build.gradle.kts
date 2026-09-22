plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

// This project lives inside a OneDrive-synced folder, and OneDrive locks
// files under app/build while it's mid-upload — Gradle then fails with
// "Unable to delete directory" / AccessDeniedException, especially on a
// clean rebuild. Redirecting build outputs to a local, non-synced folder
// avoids the race entirely instead of relying on `--stop` + manual
// `rm -rf app/build` + pausing OneDrive sync every time it happens.
val externalBuildRoot = File(
    System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir"),
    "gradle-builds/galaxy-vitals-bridge",
)
rootProject.layout.buildDirectory.set(externalBuildRoot.resolve("root"))
subprojects {
    layout.buildDirectory.set(externalBuildRoot.resolve(name))
}
