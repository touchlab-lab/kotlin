/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import org.junit.Test
import java.io.FileOutputStream
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.jdk7.test.PathTreeWalkTest.Companion.createTestFiles
import kotlin.jdk7.test.PathTreeWalkTest.Companion.referenceFilenames
import kotlin.jdk7.test.PathTreeWalkTest.Companion.referenceFilesOnly
import kotlin.jdk7.test.PathTreeWalkTest.Companion.testVisitedFiles
import kotlin.jdk7.test.PathTreeWalkTest.Companion.tryCreateSymbolicLinkTo
import kotlin.test.*

class FileVisitorBuilderTest : AbstractPathTest() {
    @Test
    fun visitOnce() {
        val basedir = createTestFiles().cleanupRecursively()

        val preVisit = hashSetOf<Path>()
        val postVisit = hashSetOf<Path>()
        val files = hashSetOf<Path>()

        val visitor = fileVisitor {
            preVisitDirectory { directory, _ ->
                assertFalse(directory in preVisit)
                if (directory == basedir) {
                    assertTrue(preVisit.isEmpty())
                } else {
                    assertTrue(directory.parent in preVisit)
                }
                preVisit.add(directory)

                FileVisitResult.CONTINUE
            }

            visitFile { file, _ ->
                assertTrue(file.parent in preVisit)
                assertFalse(file.parent in postVisit)
                files.add(file)

                FileVisitResult.CONTINUE
            }

            postVisitDirectory { directory, exception ->
                assertNull(exception)
                assertTrue(directory in preVisit)
                assertFalse(directory in postVisit)
                if (directory != basedir) {
                    assertFalse(directory.parent in postVisit)
                }
                postVisit.add(directory)

                FileVisitResult.CONTINUE
            }
        }

        basedir.visitFileTree(visitor)

        assertEquals(preVisit, postVisit)
        val referenceDirectoryNames = listOf("", "1", "1/2", "1/3", "6", "8")
        testVisitedFiles(referenceDirectoryNames, preVisit.asSequence(), basedir)
        testVisitedFiles(referenceFilesOnly, files.asSequence(), basedir)
    }

    @Test
    fun overrideOnce() {
        assertFailsWith<IllegalStateException> {
            fileVisitor {
                visitFile { _, _ -> FileVisitResult.CONTINUE }
                visitFile { _, _ -> FileVisitResult.CONTINUE }
            }
        }
    }

    @Test
    fun overrideInAlreadyBuilt() {
        val builder: FileVisitorBuilder
        fileVisitor { builder = this }
        assertFailsWith<IllegalStateException> {
            builder.visitFile { _, _ -> FileVisitResult.CONTINUE }
        }
    }

    @Test
    fun restrictedRead() {
        val basedir = createTestFiles().cleanupRecursively()
        val restrictedDir = basedir.resolve("1/3").also { it.toFile().setReadable(false) }

        assertFailsWith<AccessDeniedException> {
            val visitor = fileVisitor { }
            basedir.visitFileTree(visitor)
        }

        var didFail = false
        val visitor = fileVisitor {
            visitFileFailed { file, exception ->
                assertEquals(restrictedDir, file)
                assertIs<AccessDeniedException>(exception)

                didFail = true

                FileVisitResult.CONTINUE
            }
        }

        basedir.visitFileTree(visitor)

        assertTrue(didFail)
    }

    @Test
    fun skipDirectory() {
        val basedir = createTestFiles().cleanupRecursively()
        val dirToSkip = basedir.resolve("1/3")

        val visitor = fileVisitor {
            preVisitDirectory { directory, _ ->
                if (directory == dirToSkip)
                    FileVisitResult.SKIP_SUBTREE
                else
                    FileVisitResult.CONTINUE
            }

            visitFile { file, _ ->
                assertTrue(file.parent != dirToSkip)
                FileVisitResult.CONTINUE
            }
        }

        basedir.visitFileTree(visitor)
    }

    @Test
    fun maxDepth() {
        val basedir = createTestFiles().cleanupRecursively()

        val visitor = fileVisitor {
            visitFile { file, _ ->
                assertNotEquals("1/3/5.txt", file.relativeToOrSelf(basedir).invariantSeparatorsPathString)
                assertNotEquals("1/3/6.txt", file.relativeToOrSelf(basedir).invariantSeparatorsPathString)
                FileVisitResult.CONTINUE
            }
        }

        basedir.visitFileTree(visitor, maxDepth = 2)
    }

    @Test
    fun followLinks() {
        val basedir = createTestFiles().cleanupRecursively()
        val original = basedir.resolve("8")
        basedir.resolve("1/3/link").tryCreateSymbolicLinkTo(original) ?: return

        // directory "8" contains "9.txt" file
        var didFollowLinks = false
        val visitor = fileVisitor {
            visitFile { file, _ ->
                if (file.relativeToOrSelf(basedir).invariantSeparatorsPathString == "1/3/link/9.txt") {
                    didFollowLinks = true
                }
                FileVisitResult.CONTINUE
            }
        }

        basedir.visitFileTree(visitor)
        assertFalse(didFollowLinks)

        basedir.visitFileTree(visitor, followLinks = true)
        assertTrue(didFollowLinks)
    }

    @Test
    fun copyToRecursively() {
        val srcRoot = createTestFiles().cleanupRecursively()
        val dstRoot = createTempDirectory().cleanupRecursively()

        srcRoot.resolve("1/2/.dir").createDirectory()
        srcRoot.resolve("1/3/.file").createFile()

        val visitor = fileVisitor {
            preVisitDirectory { directory, _ ->
                if (directory.name.startsWith(".")) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    if (directory != srcRoot) {
                        val dst = dstRoot.resolve(directory.relativeTo(srcRoot))
                        directory.copyTo(dst, StandardCopyOption.COPY_ATTRIBUTES)
                    }
                    FileVisitResult.CONTINUE
                }
            }

            visitFile { file, _ ->
                if (!file.name.startsWith(".")) {
                    val dst = dstRoot.resolve(file.relativeTo(srcRoot))
                    file.copyTo(dst, StandardCopyOption.COPY_ATTRIBUTES)
                }
                FileVisitResult.CONTINUE
            }
        }

        srcRoot.visitFileTree(visitor)

        val dstWalk = dstRoot.walk(PathWalkOption.INCLUDE_DIRECTORIES)
        testVisitedFiles(referenceFilenames + listOf(""), dstWalk, dstRoot)
        val srcWalk = srcRoot.walk(PathWalkOption.INCLUDE_DIRECTORIES)
        testVisitedFiles(referenceFilenames + listOf("", "1/2/.dir", "1/3/.file"), srcWalk, srcRoot)
    }

    @Test
    fun deleteRecursively() {
        val basedir = createTestFiles()

        val visitor = fileVisitor {
            visitFile { file, _ ->
                file.deleteExisting()
                FileVisitResult.CONTINUE
            }

            postVisitDirectory { directory, _ ->
                directory.deleteExisting()
                FileVisitResult.CONTINUE
            }
        }

        basedir.visitFileTree(visitor)

        assertFalse(basedir.exists())
    }

    private fun deleteWith(excludePredicate: (Path) -> Boolean) = fileVisitor {
        preVisitDirectory { directory, _ ->
            if (excludePredicate(directory)) {
                FileVisitResult.SKIP_SUBTREE
            } else {
                FileVisitResult.CONTINUE
            }
        }

        visitFile { file, _ ->
            if (!excludePredicate(file)) {
                file.deleteExisting()
            }
            FileVisitResult.CONTINUE
        }

        postVisitDirectory { directory, _ ->
            val shouldDelete = directory.useDirectoryEntries { it.none() }
            if (shouldDelete) {
                directory.deleteExisting()
            }
            FileVisitResult.CONTINUE
        }
    }

    @Test
    fun deleteWithPredicate() {
        val basedir = createTestFiles().cleanupRecursively()
        basedir.resolve("image1.png").createFile()
        basedir.resolve("1/3/image2.png").createFile()

        val visitor = deleteWith { path ->
            when {
                path.name == "8" -> true
                path.extension == "png" -> true
                else -> false
            }
        }
        basedir.visitFileTree(visitor)

        val expected = listOf("", "1", "1/3", "1/3/image2.png", "8", "8/9.txt", "image1.png")
        testVisitedFiles(expected, basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES), basedir)
    }

    private fun zipify(rootPath: Path, zipOutputStream: ZipOutputStream): FileVisitor<Path> = fileVisitor {
        preVisitDirectory { directory, _ ->
            if (directory != rootPath) {
                val entry = ZipEntry(directory.relativeTo(rootPath).toString())

                zipOutputStream.putNextEntry(entry)
                zipOutputStream.closeEntry()
            }
            FileVisitResult.CONTINUE
        }

        visitFile { file, attributes ->
            val entry = ZipEntry(file.relativeTo(rootPath).toString())

            entry.size = attributes.size()
            zipOutputStream.putNextEntry(entry)
            Files.copy(file, zipOutputStream)
            zipOutputStream.closeEntry()

            FileVisitResult.CONTINUE
        }

        visitFileFailed { _, exception ->
            zipOutputStream.close()

            throw exception
        }
    }

    @Test
    fun archive() {
        val basedir = createTestFiles().cleanupRecursively()
        basedir.resolve("8/9.txt").writeText("ninth")

        val zipFile = createTempFile("testFiles", ".zip").toFile()
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOutputStream ->
            val visitor = zipify(basedir, zipOutputStream)
            basedir.visitFileTree(visitor)
        }

        ZipFile(zipFile).use { zip ->
            assertEquals(referenceFilenames.sorted(), zip.entries().toList().map(ZipEntry::toString).sorted())

            val ninthEntry = zip.getEntry("8/9.txt")
            zip.getInputStream(ninthEntry).bufferedReader().use { reader ->
                assertEquals("ninth", reader.readText())
            }
        }
    }
}
