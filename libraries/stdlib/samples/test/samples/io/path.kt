/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.io

import samples.*
import java.nio.file.*
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class Path {

    @Sample
    fun fileVisitor() {
        val cleanVisitor = fileVisitor {

            visitFile { file, _ ->
                if (file.parent?.name == "build") {
                    FileVisitResult.SKIP_SIBLINGS
                } else {
                    if (file.extension == "class") {
                        file.deleteExisting()
                    }
                    FileVisitResult.CONTINUE
                }
            }

            postVisitDirectory { directory, exception ->
                if (exception != null) {
                    throw exception
                }
                if (directory.name == "build") {
                    directory.toFile().deleteRecursively()
                }

                FileVisitResult.CONTINUE
            }
        }

        val rootDirectory = createTempDirectory("Project")

        rootDirectory.resolve("src").let { srcDirectory ->
            srcDirectory.createDirectory()
            srcDirectory.resolve("A.kt").createFile()
            srcDirectory.resolve("A.class").createFile()
        }
        rootDirectory.resolve("build").let { buildDirectory ->
            buildDirectory.createDirectory()
            buildDirectory.resolve("Project.jar").createFile()
        }

        val directoryStructure = rootDirectory.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .map { it.relativeTo(rootDirectory).toString() }
            .toList().sorted()
        assertPrints(directoryStructure, "[, build, build/Project.jar, src, src/A.class, src/A.kt]")

        rootDirectory.visitFileTree(cleanVisitor)

        val directoryStructureAfterClean = rootDirectory.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .map { it.relativeTo(rootDirectory).toString() }
            .toList().sorted()
        assertPrints(directoryStructureAfterClean, "[, src, src/A.kt]")
    }
}