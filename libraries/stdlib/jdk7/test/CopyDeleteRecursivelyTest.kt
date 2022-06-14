/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.nio.file.*
import kotlin.io.path.*
import kotlin.jdk7.test.PathTreeWalkTest.Companion.createTestFiles
import kotlin.jdk7.test.PathTreeWalkTest.Companion.referenceFilenames
import kotlin.jdk7.test.PathTreeWalkTest.Companion.referenceFilesOnly
import kotlin.jdk7.test.PathTreeWalkTest.Companion.testVisitedFiles
import kotlin.jdk7.test.PathTreeWalkTest.Companion.tryCreateSymbolicLinkTo
import kotlin.test.*

class CopyDeleteRecursivelyTest : AbstractPathTest() {

    @Test
    fun deleteFile() {
        val file = createTempFile()

        assertTrue(file.exists())
        file.deleteRecursively()
        assertFalse(file.exists())

        file.createFile().writeText("non-empty file")

        assertTrue(file.exists())
        file.deleteRecursively()
        assertFalse(file.exists())
        file.deleteRecursively() // successfully deletes recursively a non-existent file
    }

    @Test
    fun deleteDirectory() {
        val dir = createTestFiles()

        assertTrue(dir.exists())
        dir.deleteRecursively()
        assertFalse(dir.exists())
        dir.deleteRecursively() // successfully deletes recursively a non-existent directory
    }

    private fun Path.walkIncludeDirectories(): Sequence<Path> =
        this.walk(PathWalkOption.INCLUDE_DIRECTORIES)

    @Test
    fun deleteRestrictedRead() {
        val basedir = createTestFiles().cleanupRecursively()
        val restrictedDir = basedir.resolve("1").toFile()
        val restrictedFile = basedir.resolve("7.txt").toFile()
        try {
            if (restrictedDir.setReadable(false) && restrictedFile.setReadable(false)) {
                assertFailsWith<java.nio.file.FileSystemException>("Expected incomplete recursive deletion") {
                    basedir.deleteRecursively()
                }
                assertTrue(restrictedDir.exists()) // couldn't read directory entries
                assertFalse(restrictedFile.exists()) // restricted read allows removal of file

                restrictedDir.setReadable(true)
                testVisitedFiles(listOf("", "1", "1/2", "1/3", "1/3/4.txt", "1/3/5.txt"), basedir.walkIncludeDirectories(), basedir)
                basedir.deleteRecursively()
            } else {
                System.err.println("cannot restrict access")
            }
        } finally {
            restrictedDir.setReadable(true)
            restrictedFile.setReadable(true)
        }
    }

    @Test
    fun deleteRestrictedWrite() {
        val basedir = createTestFiles().cleanupRecursively()
        val restricted = basedir.resolve("1").toFile()
        try {
            if (restricted.setWritable(false)) {
                assertFailsWith<FileSystemException>("Expected incomplete recursive deletion") {
                    basedir.deleteRecursively()
                }
                assertTrue(restricted.exists())

                restricted.setWritable(true)
                basedir.deleteRecursively()
            } else {
                System.err.println("cannot restrict access")
            }
        } finally {
            restricted.setWritable(true)
        }
    }

    @Test
    fun deleteBaseSymlinkToFile() {
        for (followLinks in listOf(true, false)) {
            val file = createTempFile().cleanup()
            val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(file) ?: return

            link.deleteRecursively(followLinks)
            assertFalse(link.exists(LinkOption.NOFOLLOW_LINKS))
            assertTrue(file.exists())
        }
    }

    @Test
    fun deleteBaseSymlinkToDirectoryFollow() {
        val dir = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(dir) ?: return

        link.deleteRecursively(followLinks = true)
        assertFalse(link.exists(LinkOption.NOFOLLOW_LINKS))
        assertEquals(dir, dir.walkIncludeDirectories().single())
    }

    @Test
    fun deleteBaseSymlinkToDirectoryNoFollow() {
        val dir = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(dir) ?: return

        link.deleteRecursively(followLinks = false)
        assertFalse(link.exists(LinkOption.NOFOLLOW_LINKS))
        testVisitedFiles(listOf("") + referenceFilenames, dir.walkIncludeDirectories(), dir)
    }

    @Test
    fun deleteFollowSymlinks() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(dir1) ?: return }

        dir2.deleteRecursively(followLinks = true)
        assertFalse(dir2.exists())
        assertEquals(dir1, dir1.walkIncludeDirectories().single())
    }

    @Test
    fun deleteNoFollowSymlinks() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(dir1) ?: return }

        dir2.deleteRecursively(followLinks = false)
        assertFalse(dir2.exists())
        testVisitedFiles(listOf("") + referenceFilenames, dir1.walkIncludeDirectories(), dir1)
    }

    @Test
    fun deleteSymlinkToSymlink() {
        val dir = createTestFiles()
        val link = createTempDirectory().resolve("link").tryCreateSymbolicLinkTo(dir) ?: return
        val linkToLink = createTempDirectory().resolve("linkToLink").tryCreateSymbolicLinkTo(link) ?: return

        linkToLink.deleteRecursively(followLinks = true)
        assertFalse(linkToLink.exists(LinkOption.NOFOLLOW_LINKS))
        assertTrue(link.exists(LinkOption.NOFOLLOW_LINKS)) // the mediator symlink is not deleted
        assertEquals(dir, dir.walkIncludeDirectories().single())
    }

    @Test
    fun deleteSymlinkCyclic() {
        val basedir = createTestFiles().cleanupRecursively()
        val original = basedir.resolve("1")
        original.resolve("2/link").tryCreateSymbolicLinkTo(original) ?: return

        assertFailsWith<java.nio.file.FileSystemException> {
            basedir.deleteRecursively(followLinks = true)
        }.let { exception ->
            assertEquals(4, exception.suppressedExceptions.size)
            // a loop was detected
            assertIs<java.nio.file.FileSystemLoopException>(exception.suppressedExceptions[0])
            // directories from root to the file where the loop was detected
            assertTrue(exception.suppressedExceptions.drop(1).all { it is java.nio.file.DirectoryNotEmptyException })
        }
        assertTrue(basedir.exists()) // partial delete have taken place

        basedir.deleteRecursively(followLinks = false)
        assertFalse(basedir.exists())
    }
/*
    @Test
    fun deleteSymlinkCyclicWithTwo() {
        val basedir = createTestFiles().cleanupRecursively()
        val dir8 = basedir.resolve("8")
        val dir2 = basedir.resolve("1/2")
        dir8.resolve("linkTo2").tryCreateSymbolicLinkTo(dir2) ?: return
        dir2.resolve("linkTo8").tryCreateSymbolicLinkTo(dir8) ?: return

        assertFailsWith<java.nio.file.FileSystemException> {
            basedir.deleteRecursively(followLinks = true)
        }.let { exception ->
            assertEquals(2, exception.suppressedExceptions.filterIsInstance<java.nio.file.FileSystemLoopException>().size)
        }
        assertTrue(basedir.exists()) // partial delete have taken place

        basedir.deleteRecursively(followLinks = false)
        assertFalse(basedir.exists())
    }
*/

    @Test
    fun deleteSymlinkPointingToItself() {
        val basedir = createTempDirectory().cleanupRecursively()
        val link = basedir.resolve("link")
        link.tryCreateSymbolicLinkTo(link) ?: return

//        assertFailsWith<java.nio.file.FileSystemException> { // Thrown from readAttributes: "Too many levels of symbolic links"
        basedir.deleteRecursively(followLinks = true)
//        }
        assertFalse(basedir.exists())
    }

    @Test
    fun deleteSymlinkTwoPointingToEachOther() {
        val basedir = createTempDirectory().cleanupRecursively()
        val link1 = basedir.resolve("link1")
        val link2 = basedir.resolve("link2").tryCreateSymbolicLinkTo(link1) ?: return
        link1.tryCreateSymbolicLinkTo(link2) ?: return

//        assertFailsWith<java.nio.file.FileSystemException> { // Thrown from readAttributes: "Too many levels of symbolic links"
        basedir.deleteRecursively(followLinks = true)
//        }
        assertFalse(basedir.exists())
    }

    private fun compareFiles(src: Path, dst: Path, message: String? = null) {
        assertTrue(dst.exists())
        assertEquals(src.isRegularFile(), dst.isRegularFile(), message)
        assertEquals(src.isDirectory(), dst.isDirectory(), message)
        if (dst.isRegularFile()) {
            assertTrue(src.readBytes().contentEquals(dst.readBytes()), message)
        }
    }

    private fun compareDirectories(src: Path, dst: Path) {
        for (srcFile in src.walkIncludeDirectories()) {
            val dstFile = dst.resolve(srcFile.relativeTo(src))
            compareFiles(srcFile, dstFile)
        }
    }

    @Test
    fun copyFileToFile() {
        val src = createTempFile().cleanup().also { it.writeText("hello") }
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        src.copyToRecursively(dst)
        compareFiles(src, dst)

        dst.writeText("bye")
        val error = assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst)
        }
        assertIs<java.nio.file.FileAlreadyExistsException>(error.suppressedExceptions.single())
        assertEquals("bye", dst.readText())

        src.copyToRecursively(dst) { source, target -> source.copyTo(target, overwrite = true) }
        compareFiles(src, dst)
    }

    @Test
    fun copyFileToDirectory() {
        val src = createTempFile().cleanup().also { it.writeText("hello") }
        val dst = createTestFiles().cleanupRecursively()

        val existsError = assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst)
        }
        assertIs<java.nio.file.FileAlreadyExistsException>(existsError.suppressedExceptions.single())
        assertTrue(dst.isDirectory())

        val notEmptyException = assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst) { source, target -> source.copyTo(target, overwrite = true) }
        }
        assertIs<java.nio.file.DirectoryNotEmptyException>(notEmptyException.suppressedExceptions.single())
        assertTrue(dst.isDirectory())

        dst.deleteRecursively()
        dst.createDirectory()
        src.copyToRecursively(dst) { source, target -> source.copyTo(target, overwrite = true) }
        compareFiles(src, dst)
    }

    @Test
    fun copyDirectoryToDirectory() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        src.copyToRecursively(dst)
        compareDirectories(src, dst)

        src.resolve("1/3/4.txt").writeText("hello")
        dst.resolve("10").createDirectory()
        assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst)
        }.let { exception ->
            assertEquals(referenceFilesOnly.size, exception.suppressedExceptions.size)
            assertTrue(exception.suppressedExceptions.all { it is java.nio.file.FileAlreadyExistsException })
        }
        assertTrue(dst.resolve("1/3/4.txt").readText().isEmpty())

        assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst) { source, target ->
                source.copyTo(target, overwrite = true)
            }
        }.let { exception ->
            // non-empty directories: "", "1", "1/3", "8"
            assertEquals(4, exception.suppressedExceptions.size)
            assertTrue(exception.suppressedExceptions.all { it is java.nio.file.DirectoryNotEmptyException })
        }
        assertTrue(dst.resolve("10").exists())
        compareDirectories(src, dst)
    }

    @Test
    fun copyDirectoryToFile() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempFile().cleanupRecursively().also { it.writeText("hello") }

        assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst)
        }.let { exception ->
            // attempted to copy each file from src to dst, where a file already exists
            assertEquals(referenceFilenames.size + 1 /* root dir */, exception.suppressedExceptions.size)
            assertIs<java.nio.file.FileAlreadyExistsException>(exception.suppressedExceptions[0])
            assertTrue(exception.suppressedExceptions.drop(1).all { it is java.nio.file.FileSystemException }) // dst is "Not a directory"
        }
        assertTrue(dst.isRegularFile())

        src.copyToRecursively(dst) { source, target -> source.copyTo(target, overwrite = true) }
        compareDirectories(src, dst)
    }

    @Test
    fun copyNonExistentSource() {
        val src = createTempDirectory().also { it.deleteExisting() }
        val dst = createTempDirectory()

        assertFailsWith(NoSuchFileException::class) {
            src.copyToRecursively(dst)
        }

        dst.deleteExisting()
        assertFailsWith(NoSuchFileException::class) {
            src.copyToRecursively(dst)
        }

//        assertTrue(src.copyToRecursively(dst) { _, _ -> OnErrorAction.SKIP })
//        assertFalse(src.copyToRecursively(dst) { _, _ -> OnErrorAction.TERMINATE })
    }

    @Test
    fun copyNonExistentDestinationParent() {
        val src = createTempDirectory().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("parent/dst")

        assertFalse(dst.parent.exists())

        val error = assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst)
        }
        assertIs<java.nio.file.NoSuchFileException>(error.suppressed.single())

        src.copyToRecursively(dst.apply { parent?.createDirectories() })
    }

    @Test
    fun copyWithConflicts() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTestFiles().cleanupRecursively()

        dst.resolve("8").deleteRecursively()
        val existingNames = hashSetOf<String>()
        src.copyToRecursively(dst) { source, target ->
            try {
                if (!source.isDirectory() || !target.isDirectory())
                    source.copyTo(target)
            } catch (exception: java.nio.file.FileAlreadyExistsException) {
                existingNames.add(target.relativeToOrSelf(dst).invariantSeparatorsPathString)
                // ignore exception
            }
        }
        assertEquals(setOf("1/3/4.txt", "1/3/5.txt", "7.txt"), existingNames)
        compareDirectories(src, dst)
    }

    @Test
    fun copyRestrictedRead() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively()

        val restricted = src.resolve("1/3").toFile()
        if (restricted.setReadable(false)) {
            try {
                val error = assertFailsWith<java.nio.file.FileSystemException> {
                    src.copyToRecursively(dst)
                }
                assertIs<java.nio.file.AccessDeniedException>(error.suppressedExceptions.single())

                assertFalse(dst.resolve("1/3").isReadable()) // access permissions are copied
            } finally {
                restricted.setReadable(true)
            }
        } else {
            System.err.println("cannot restrict access")
        }
    }

    // TODO: Fix cleanupRecursively that fails when a restricted source is copied
    @Test
    fun copyRestrictedWriteInSource() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively()

        val restricted = src.resolve("1/3").toFile()
        if (restricted.setWritable(false)) {
            try {
                val error = assertFailsWith<java.nio.file.FileSystemException> {
                    src.copyToRecursively(dst)
                }
                error.suppressedExceptions.forEach {
                    assertIs<java.nio.file.AccessDeniedException>(it)
                    assertTrue(it.file.endsWith("4.txt") || it.file.endsWith("5.txt"))
                }
            } finally {
                restricted.setReadable(true)
            }
        } else {
            System.err.println("cannot restrict access")
        }
    }

    @Test
    fun copyRestrictedWrite() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTestFiles().cleanupRecursively()

        src.resolve("1/3/4.txt").writeText("hello")
        src.resolve("7.txt").writeText("hi")

        val restricted = dst.resolve("1/3").toFile()
        if (restricted.setWritable(false)) {
            try {
                assertFailsWith<java.nio.file.FileSystemException> {
                    src.copyToRecursively(dst) { source, target ->
                        if (!source.isDirectory() || !target.isDirectory()) source.copyTo(target, overwrite = true)
                    }
                }.suppressedExceptions.let { suppressed ->
                    // restricted to overwrite: "1/3/4.txt", "1/3/5.txt"
                    assertEquals(2, suppressed.size)
                    assertTrue { suppressed.all { it is java.nio.file.AccessDeniedException } }
                }

                assertNotEquals(src.resolve("1/3/4.txt").readText(), dst.resolve("1/3/4.txt").readText())
            } finally {
                restricted.setWritable(true)
            }

            src.resolve("1/3").deleteRecursively()
            dst.resolve("1/3").deleteRecursively()
            compareDirectories(src, dst)
        } else {
            System.err.println("cannot restrict access")
        }
    }

    @Test
    fun copyBaseSymlinkPointingToFileFollow() {
        val src = createTempFile().cleanup().also { it.writeText("hello") }
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        link.copyToRecursively(dst, followLinks = true)
        compareFiles(src, dst)
    }

    @Test
    fun copyBaseSymlinkPointingToFileNoFollow() {
        val src = createTempFile().cleanup().also { it.writeText("hello") }
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        link.copyToRecursively(dst, followLinks = false)
        compareFiles(link, dst)
    }

    @Test
    fun copyBaseSymlinkPointingToDirectoryFollow() {
        val src = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        link.copyToRecursively(dst, followLinks = true)
        compareDirectories(src, dst)
    }

    @Test
    fun copyBaseSymlinkPointingToDirectoryNoFollow() {
        val src = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        link.copyToRecursively(dst, followLinks = false)
        compareFiles(link, dst)
    }

    @Test
    fun copyFollowSymlinks() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(dir1) ?: return }
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        dir2.copyToRecursively(dst, followLinks = true)
        val dir2Content = listOf("", "8/link") + referenceFilenames
        val expectedDstContent = dir2Content + referenceFilenames.map { "8/link/$it" }
        testVisitedFiles(expectedDstContent, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copyNoFollowSymlinks() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(dir1) ?: return }
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        dir2.copyToRecursively(dst, followLinks = false)
        testVisitedFiles(listOf("", "8/link") + referenceFilenames, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copyOverwriteFollowSymlinks() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(dir1) ?: return }

        val dir3 = createTempDirectory().cleanupRecursively().also { it.resolve("file.txt").createFile() }
        val dst = createTempDirectory().cleanupRecursively().also { it.resolve("1").tryCreateSymbolicLinkTo(dir3) ?: return }

        dir2.copyToRecursively(dst, followLinks = true) { source, target ->
            if (!source.isDirectory() || !target.isDirectory(LinkOption.NOFOLLOW_LINKS))
                source.copyTo(target, overwrite = true)
        }

        // the dir pointed from dst is not deleted
        testVisitedFiles(listOf("", "file.txt"), dir3.walkIncludeDirectories(), dir3)

        // content of the directory pointed from src is copied
        val dir2Content = listOf("", "8/link") + referenceFilenames
        val expectedDstContent = dir2Content + referenceFilenames.map { "8/link/$it" }
        testVisitedFiles(expectedDstContent, dst.walkIncludeDirectories(), dst)

        // symlink from dst is overwritten
        assertFalse(dst.resolve("1").isSymbolicLink())
    }

    @Test
    fun copyOverwriteNoFollowSymlinks() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(dir1) ?: return }

        val dir3 = createTempDirectory().cleanupRecursively().also { it.resolve("file.txt").createFile() }
        val dst = createTempDirectory().cleanupRecursively().also { it.resolve("7.txt").tryCreateSymbolicLinkTo(dir3) ?: return }

        dir2.copyToRecursively(dst, followLinks = false) { source, target ->
            if (!source.isDirectory(LinkOption.NOFOLLOW_LINKS) || !target.isDirectory(LinkOption.NOFOLLOW_LINKS))
                source.copyTo(target, overwrite = true)
        }

        // the dir pointed from dst is not deleted
        testVisitedFiles(listOf("", "file.txt"), dir3.walkIncludeDirectories(), dir3)

        // content of the directory pointed from src is not copied
        testVisitedFiles(listOf("", "8/link") + referenceFilenames, dst.walkIncludeDirectories(), dst)

        // symlink from dst is overwritten
        assertFalse(dst.resolve("7.txt").isSymbolicLink())
    }

    @Test
    fun copySymlinkToSymlink() {
        val src = createTestFiles()
        val link = createTempDirectory().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val linkToLink = createTempDirectory().resolve("linkToLink").tryCreateSymbolicLinkTo(link) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        linkToLink.copyToRecursively(dst, followLinks = true)
        testVisitedFiles(listOf("") + referenceFilenames, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copySymlinkCyclic() {
        val src = createTestFiles().cleanupRecursively()
        val original = src.resolve("1")
        original.resolve("2/link").tryCreateSymbolicLinkTo(original) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        val error = assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst, followLinks = true)
        }
        assertIs<java.nio.file.FileSystemLoopException>(error.suppressedExceptions.single())
        // partial copy
        val copiedFiles = dst.walkIncludeDirectories().toList()
        assertEquals(src.walkIncludeDirectories().count() - 1, copiedFiles.size) // only "1/2/link" is not copied
        val pathToLink = listOf(dst.resolve("1"), dst.resolve("1/2"))
        assertTrue(copiedFiles.containsAll(pathToLink))
    }

    @Test
    fun copySymlinkCyclicWithTwo() {
        val src = createTestFiles().cleanupRecursively()
        val dir8 = src.resolve("8")
        val dir2 = src.resolve("1/2")
        dir8.resolve("linkTo2").tryCreateSymbolicLinkTo(dir2) ?: return
        dir2.resolve("linkTo8").tryCreateSymbolicLinkTo(dir8) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst, followLinks = true)
        }.let { exception ->
            // a cycle detected from both of its directions
            assertEquals(2, exception.suppressedExceptions.size)
            assertTrue(exception.suppressedExceptions.all { it is java.nio.file.FileSystemLoopException })
        }
        // partial copy
        assertTrue(dst.exists())
    }

    @Test
    fun copySymlinkPointingToItself() {
        val src = createTempDirectory().cleanupRecursively()
        val link = src.resolve("link")
        link.tryCreateSymbolicLinkTo(link) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        assertFailsWith<java.nio.file.FileSystemException> {
            // throws with message "Too many levels of symbolic links"
            src.copyToRecursively(dst, followLinks = true)
        }
    }

    @Test
    fun copySymlinkTwoPointingToEachOther() {
        val src = createTempDirectory().cleanupRecursively()
        val link1 = src.resolve("link1")
        val link2 = src.resolve("link2").tryCreateSymbolicLinkTo(link1) ?: return
        link1.tryCreateSymbolicLinkTo(link2) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        assertFailsWith<java.nio.file.FileSystemException> {
            // throws with message "Too many levels of symbolic links"
            src.copyToRecursively(dst, followLinks = true)
        }
    }

    @Test
    fun canDeleteCurrentlyOpenDirectory() {
        val basedir = createTempDirectory().cleanupRecursively()
        val relativePath = basedir.relativeTo(basedir)
        Files.newDirectoryStream(basedir).use { directoryStream ->
            if (directoryStream is SecureDirectoryStream) {
                println("Secure, relativePath: $relativePath")
                directoryStream.deleteDirectory(relativePath)
            } else {
                println("Insecure, relativePath: $relativePath")
                basedir.deleteIfExists()
            }
        }
        println("Was deleted: ${basedir.notExists()}")
    }

    @Test
    fun isDirectoryEntryInRelativePath() {
        val basedir = createTestFiles().cleanupRecursively()
        Files.newDirectoryStream(basedir).use { directoryStream ->
            if (directoryStream is SecureDirectoryStream) {
                println("Secure")
            } else {
                println("Not secure")
            }
            directoryStream.forEach {
                println(it)
            }
        }
    }
}
