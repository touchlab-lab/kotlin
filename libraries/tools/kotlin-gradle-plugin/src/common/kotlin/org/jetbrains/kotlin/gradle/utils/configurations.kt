/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider

const val COMPILE_ONLY = "compileOnly"
const val COMPILE = "compile"
const val IMPLEMENTATION = "implementation"
const val API = "api"
const val RUNTIME_ONLY = "runtimeOnly"
const val RUNTIME = "runtime"
internal const val INTRANSITIVE = "intransitive"

private class UnserializableLazy<T: Any>(
    private val initializer: () -> T
) : Lazy<T> {
    @Volatile
    @Transient
    private var _value: T? = null
    override fun isInitialized(): Boolean = _value != null
    override val value get(): T {
        val v1 = _value
        if (v1 != null) return v1

        return synchronized(this) {
            val v2 = _value
            if (v2 == null) {
                 initializer().also { _value = it }
            } else {
                v2
            }
        }
    }
}

/**
 * Gradle Configuration Cache-friendly representation of resolved Configuration
 */
internal class ResolvedDependencyGraph
private constructor (
    private val graphRootProvider: Provider<ResolvedComponentResult>,
    private val artifactCollection: ArtifactCollection
) {
    val root get() = graphRootProvider.get()
    val files: FileCollection get() = artifactCollection.artifactFiles
    val artifacts get() = artifactCollection.artifacts

    private val artifactsByComponentId by UnserializableLazy { artifacts.groupBy { it.id.componentIdentifier } }

    val allDependencies: List<DependencyResult> get() {
        fun DependencyResult.allDependenciesRecursive(): List<DependencyResult> =
            if (this is ResolvedDependencyResult) {
                listOf(this) + selected.dependencies.flatMap { it.allDependenciesRecursive() }
            } else {
                listOf(this)
            }

        return root.dependencies.flatMap { it.allDependenciesRecursive() }
    }

    fun dependencyArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        val componentId = dependency.selected.id
        return artifactsByComponentId[componentId] ?: emptyList()
    }

    companion object {
        operator fun invoke(project: Project, configuration: Configuration) = ResolvedDependencyGraph(
            graphRootProvider = configuration.incoming.resolutionResult.let { rr -> project.provider { rr.root } },
            artifactCollection = configuration.incoming.artifacts
        )
    }
}