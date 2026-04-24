// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        constraints {
            classpath("org.jdom:jdom2:2.0.6.1") {
                because("fix XXE vulnerability in AGP transitive dependency (CVE-2021-33813)")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.google.firebase.perf) apply false
}