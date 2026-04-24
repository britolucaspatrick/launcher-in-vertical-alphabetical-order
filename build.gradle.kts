// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        constraints {
            classpath("org.jdom:jdom2:2.0.6.1") {
                because("fix XXE vulnerability in AGP transitive dependency (CVE-2021-33813)")
            }
            classpath("io.netty:netty-codec-http2:4.1.132.Final") {
                because("fix HTTP/2 CONTINUATION Frame Flood DoS in transitive dependency")
            }
            classpath("org.bitbucket.b_c:jose4j:0.9.6") {
                because("fix DoS via compressed JWE content in transitive dependency")
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