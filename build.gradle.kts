// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.netty" && requested.name.startsWith("netty-")) {
                useVersion("4.1.132.Final")
                because("fix multiple security vulnerabilities including HTTP/2 CONTINUATION Frame Flood DoS")
            }
        }
    }
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
            classpath("org.apache.commons:commons-lang3:3.20.0") {
                because("fix Uncontrolled Recursion vulnerability in transitive dependency")
            }
            classpath("org.bouncycastle:bcpkix-jdk18on:1.84") {
                because("fix broken or risky cryptographic algorithm vulnerability")
            }
            classpath("org.bouncycastle:bcprov-jdk18on:1.84") {
                because("ensure consistency with bcpkix and fix potential vulnerabilities")
            }
            classpath("org.apache.httpcomponents:httpclient:4.5.14") {
                because("fix XSS vulnerability in transitive dependency (CVE-2020-13956)")
            }
            classpath("com.google.guava:guava:33.6.0-android") {
                because("fix Information Disclosure vulnerability in transitive dependency (CVE-2023-2976)")
            }
        }
    }
}

subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.netty" && requested.name.startsWith("netty-")) {
                useVersion("4.1.132.Final")
                because("fix multiple security vulnerabilities including HTTP/2 CONTINUATION Frame Flood DoS")
            }
            if (requested.group == "org.jdom" && requested.name == "jdom2") {
                useVersion("2.0.6.1")
                because("fix XXE vulnerability (CVE-2021-33813)")
            }
            if (requested.group == "org.bitbucket.b_c" && requested.name == "jose4j") {
                useVersion("0.9.6")
                because("fix DoS via compressed JWE content (CVE-2023-31582)")
            }
            if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
                useVersion("3.20.0")
                because("fix Uncontrolled Recursion vulnerability (CVE-2024-34447)")
            }
            if (requested.group == "org.bouncycastle" && requested.name.startsWith("bc") && requested.name.endsWith("-jdk18on")) {
                useVersion("1.84")
                because("fix broken or risky cryptographic algorithm vulnerability")
            }
            if (requested.group == "org.apache.httpcomponents" && requested.name == "httpclient") {
                useVersion("4.5.14")
                because("fix XSS vulnerability (CVE-2020-13956)")
            }
            if (requested.group == "com.google.guava" && requested.name == "guava") {
                useVersion("33.6.0-android")
                because("fix Information Disclosure vulnerability (CVE-2023-2976)")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.google.firebase.perf) apply false
    alias(libs.plugins.google.firebase.appdistribution) apply false
}