plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.hongyan"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 使用 gradle.properties 中的配置，支持版本向上兼容
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        instrumentationTools()
        
        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        bundledPlugin("com.intellij.java")
    }
    
    // JUnit 5 dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with (it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n")
            }
        }

        // Get the latest available change notes from the changelog file
        changeNotes.set(providers.gradleProperty("pluginVersion").map { pluginVersion ->
            "Change notes for version $pluginVersion"
        })

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn("patchChangelog")
    }
    
    // 添加运行测试程序的任务
    register<JavaExec>("runSimpleJsonTest") {
        group = "application"
        description = "Run SimpleJsonTest"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.SimpleJsonTest")
    }
    
    register<JavaExec>("runTestInvokeFormat") {
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.TestInvokeFormat")
    }
    
    register<JavaExec>("runFieldGenerationTest") {
        group = "application"
        description = "Run FieldGenerationTest"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.FieldGenerationTest")
    }
    
    register<JavaExec>("runDubboCommandGeneratorTest") {
        group = "application"
        description = "Run DubboCommandGeneratorTest"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.DubboCommandGeneratorTest")
    }
    
    register<Test>("runAllTests") {
        group = "verification"
        description = "Run all tests"
        useJUnitPlatform()
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
    
    register<JavaExec>("runTypeResolverTest") {
        group = "verification"
        description = "Run TypeResolver test"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.TypeResolverTest")
    }
    
    register<JavaExec>("runNestedObjectTest") {
        group = "application"
        description = "Run NestedObjectTest"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.NestedObjectTest")
    }
    
    register<JavaExec>("runInnerClassListTest") {
        group = "application"
        description = "Run InnerClassListTest"
        classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.test.InnerClassListTest")
    }
    
    register<Test>("runListRowTest") {
        group = "verification"
        description = "Run ListRowTest"
        useJUnitPlatform()
        include("**/ListRowTest.class")
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
    
    register<JavaExec>("testListRowParsing") {
        group = "verification"
        description = "Test List<Row> inner class parsing"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.TestListRowParsing")
    }
}