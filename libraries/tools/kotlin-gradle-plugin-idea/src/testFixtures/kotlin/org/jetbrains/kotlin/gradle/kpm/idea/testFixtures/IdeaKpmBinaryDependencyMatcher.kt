/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmResolvedBinaryDependency
import java.io.File

fun buildIdeaKpmBinaryDependencyMatchers(notation: Any?): List<IdeaKpmBinaryDependencyMatcher> {
    return when (notation) {
        null -> emptyList()
        is IdeaKpmBinaryDependencyMatcher -> listOf(notation)
        is String -> listOf(IdeaKpmBinaryDependencyMatcher.Coordinates(parseIdeaKpmBinaryCoordinates(notation)))
        is Regex -> listOf(IdeaKpmBinaryDependencyMatcher.CoordinatesRegex(notation))
        is File -> listOf(IdeaKpmBinaryDependencyMatcher.BinaryFile(notation))
        is Iterable<*> -> notation.flatMap { child -> buildIdeaKpmBinaryDependencyMatchers(child) }
        else -> error("Can't build ${IdeaKpmBinaryDependencyMatcher::class.simpleName} from $notation")
    }
}

interface IdeaKpmBinaryDependencyMatcher : IdeaKpmDependencyMatcher<IdeaKpmBinaryDependency>{
    class Coordinates(
        private val coordinates: IdeaKpmBinaryCoordinates
    ) : IdeaKpmBinaryDependencyMatcher {
        override val description: String = coordinates.toString()

        override fun matches(dependency: IdeaKpmBinaryDependency): Boolean {
            return coordinates == dependency.coordinates
        }
    }

    class CoordinatesRegex(
        private val regex: Regex
    ) : IdeaKpmBinaryDependencyMatcher {
        override val description: String = regex.pattern

        override fun matches(dependency: IdeaKpmBinaryDependency): Boolean {
            return regex.matches(dependency.coordinates.toString())
        }
    }

    class BinaryFile(
        private val binaryFile: File
    ) : IdeaKpmBinaryDependencyMatcher {
        override val description: String = binaryFile.path

        override fun matches(dependency: IdeaKpmBinaryDependency): Boolean {
            return dependency is IdeaKpmResolvedBinaryDependency && dependency.binaryFile == binaryFile
        }
    }

    class InDirectory(
        private val parentFile: File
    ) : IdeaKpmBinaryDependencyMatcher {
        constructor(parentFilePath: String) : this(File(parentFilePath))

        override val description: String = "$parentFile/**"

        override fun matches(dependency: IdeaKpmBinaryDependency): Boolean {
            return dependency is IdeaKpmResolvedBinaryDependency &&
                    dependency.binaryFile.absoluteFile.normalize().canonicalPath.startsWith(
                        parentFile.absoluteFile.normalize().canonicalPath
                    )
        }
    }
}
