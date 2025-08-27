plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.hongyan"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // 使用简化的Dubbo实现，避免扩展机制问题
    implementation("com.alibaba:dubbo:2.6.12")
    
    // 完整的ZooKeeper客户端依赖
    implementation("org.apache.curator:curator-framework:2.13.0")
    implementation("org.apache.curator:curator-client:2.13.0")
    implementation("org.apache.curator:curator-recipes:2.13.0")
    implementation("org.apache.zookeeper:zookeeper:3.4.14") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j", module = "log4j")
    }
    
    // 标准序列化支持（Dubbo内部使用）
    implementation("com.alibaba:hessian-lite:3.2.13")
    
    // 工具类
    implementation("org.javassist:javassist:3.29.2-GA")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.51")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    

    // Logging - 使用 IntelliJ 内置的日志系统
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    
    // JUnit 5 dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

intellij {
    version = providers.gradleProperty("platformVersion")
    type = providers.gradleProperty("platformType")
    
    plugins = providers.gradleProperty("platformPlugins").map { plugins ->
        if (plugins.isEmpty()) emptyList() else plugins.split(",").map { it.trim() }
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

    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        untilBuild.set(providers.gradleProperty("pluginUntilBuild"))
    }
    
    publishPlugin {
        dependsOn("patchChangelog")
    }
    
    // 配置 jar 任务包含所有依赖
    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory()) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        
        // 排除签名文件避免冲突
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/MANIFEST.MF")
        
        // 确保包含Dubbo相关的类与资源
        include("**/*.class")
        include("**/*.properties")
        include("**/*.xml")
        include("META-INF/services/**")
        include("META-INF/dubbo/**")
        // 关键：包含 Dubbo 的序列化白名单/黑名单等资源
        include("security/**")
        
        // 添加manifest配置，确保类加载器可见性
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.name }
            )
        }
    }
    
    // 添加运行测试程序的任务
    register<JavaExec>("runSimplifiedDubboTest") {
        group = "verification"
        description = "Run SimplifiedDubboTest to verify the fixed implementation"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.SimplifiedDubboTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
    
    register<JavaExec>("runOptimizedDubboTest") {
        group = "verification"
        description = "Run OptimizedDubboTest to verify all improvements"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.OptimizedDubboTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
    
    register<JavaExec>("runHttpFallbackTest") {
        group = "verification"
        description = "Run HttpFallbackTest to verify the HTTP fallback mechanism"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.HttpFallbackTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
    
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
    
    // 最终修复测试
    register<JavaExec>("runFinalFixTest") {
        group = "verification"
        description = "Run final fix test to verify ZooKeeper dependencies and native Dubbo protocol improvements"
        classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.FinalFixTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
    
    // 直连冒烟测试（带 JDK17 --add-opens 以兼容 Hessian2 反射）
    register<JavaExec>("runDirectInvokeSmokeTest") {
        group = "verification"
        description = "Run DirectInvokeSmokeTest with JDK17 --add-opens"
        classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.DirectInvokeSmokeTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
    
    // 详细的Dubbo调用测试
    register<JavaExec>("runDetailedDubboTest") {
        group = "verification"
        description = "Run detailed Dubbo test with various parameter types"
        classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.DetailedDubboTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
    
    // 修正后的Dubbo调用测试
    register<JavaExec>("runFixedDubboTest") {
        group = "verification"
        description = "Run fixed Dubbo test with correct parameter types"
        classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.FixedDubboTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
    
    // 最终修复验证测试
    register<JavaExec>("runUltimateFixTest") {
        group = "verification"
        description = "Run ultimate fix verification test to check JSON format and multi-layer fallback mechanism"
        classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath
        mainClass.set("com.hongyan.dubboinvoke.UltimateFixTest")
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
}