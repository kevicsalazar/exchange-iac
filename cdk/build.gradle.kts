plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

group = "cdk"

application {
    mainClass.set("cdk.AppKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.aws.cdk)
}
