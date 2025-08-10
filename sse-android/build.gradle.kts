// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  // Compose plugin not available on Kotlin 1.8.22; handled by composeOptions in module if needed
  // alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.android.library) apply false
}