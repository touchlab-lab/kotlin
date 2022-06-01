import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

description = "Atomicfu Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("de.undercouch.download")
}

val antLauncherJar by configurations.creating
val testJsRuntime by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}

val atomicfuJsClasspath by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val atomicfuJvmClasspath by configurations.creating

val atomicfuJsIrRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))

    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.translator"))
    compileOnly(project(":compiler:backend.js"))

    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree.impl"))

    compileOnly(kotlinStdlib())

    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))

    testImplementation(projectTests(":js:js.tests"))
    testApi(commonDependency("junit:junit"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))

    testRuntimeOnly(kotlinStdlib())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(project(":compiler:backend-common"))
    testRuntimeOnly(commonDependency("org.fusesource.jansi", "jansi"))

    atomicfuJsClasspath("org.jetbrains.kotlinx:atomicfu-js:0.17.1") { isTransitive = false }
    atomicfuJvmClasspath("org.jetbrains.kotlinx:atomicfu:0.17.1") { isTransitive = false }
    atomicfuJsIrRuntimeForTests(project(":kotlinx-atomicfu-runtime"))  { isTransitive = false }

    embedded(project(":kotlinx-atomicfu-runtime")) {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
            attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        }
        isTransitive = false
    }

    testImplementation("org.jetbrains.kotlinx:atomicfu:0.17.1")

    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.6.2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xphases-to-dump=JvmLocalDeclarations"
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
    workingDir = rootDir
    dependsOn(atomicfuJsIrRuntimeForTests)
    doFirst {
        systemProperty("atomicfuJsIrRuntimeForTests.classpath", atomicfuJsIrRuntimeForTests.asPath)
        systemProperty("atomicfuJs.classpath", atomicfuJsClasspath.asPath)
        systemProperty("atomicfuJvm.classpath", atomicfuJvmClasspath.asPath)
    }
    setUpJsIrBoxTests()
}

fun Test.setupV8() {
    dependsOn(":js:js.tests:unzipV8")
    doFirst {
        val unzipV8Task = project.tasks.getByPath(":js:js.tests:unzipV8")
        systemProperty("javascript.engine.path.V8", File(unzipV8Task.outputs.files.single().path, "d8"))
    }
}

fun Test.setUpJsIrBoxTests() {
    setupV8()

    dependsOn(":dist")
    dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
    systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/js-ir/build/classes/kotlin/js/main")
    dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
    systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
    dependsOn(":kotlin-test:kotlin-test-js-ir:compileKotlinJs")
    systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
    systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
    systemProperty("kotlin.js.test.root.out.dir", "$buildDir/")
}
