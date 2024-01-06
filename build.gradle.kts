plugins {
    id("java")

    // problems with java 21 support, using working fork for now
//    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.github.goooler.shadow") version "8.1.2"
}

group = "me.squidxtv"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.j256.ormlite:ormlite-jdbc:6.1") // instead of hibernate
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.58.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}


tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    relocate("co.aikar.commands", "me.squidxtv.groupingsystem.acf")
    relocate("co.aikar.locales", "me.squidxtv.groupingsystem.locales")
}

tasks.build {
    dependsOn("shadowJar")
}