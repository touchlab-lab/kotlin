/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * The builder to provide implementation of the file visitor that [fileVisitor] builds.
 *
 * @sample samples.io.Path.fileVisitor
 */
@ExperimentalPathApi
@SinceKotlin("1.7")
public sealed interface FileVisitorBuilder {
    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, [FileVisitor.preVisitDirectory] of the built file visitor returns [FileVisitResult.CONTINUE].
     */
    public fun preVisitDirectory(function: (directory: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, [FileVisitor.visitFile] of the built file visitor returns [FileVisitResult.CONTINUE].
     */
    public fun visitFile(function: (file: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, [FileVisitor.visitFileFailed] of the built file visitor re-throws the I/O exception
     * that prevented the file from being visited.
     */
    public fun visitFileFailed(function: (file: Path, exception: IOException) -> FileVisitResult): Unit

    /**
     * Overrides the corresponding function of the built file visitor with the provided [function].
     *
     * By default, if the directory iteration completes without an I/O exception,
     * [FileVisitor.postVisitDirectory] of the built file visitor returns [FileVisitResult.CONTINUE];
     * otherwise it re-throws the I/O exception that caused the iteration of the directory to terminate prematurely.
     */
    public fun postVisitDirectory(function: (directory: Path, exception: IOException?) -> FileVisitResult): Unit
}


@ExperimentalPathApi
internal class FileVisitorBuilderImpl : FileVisitorBuilder {
    private var preVisitDirectory: ((Path, BasicFileAttributes) -> FileVisitResult)? = null
    private var visitFile: ((Path, BasicFileAttributes) -> FileVisitResult)? = null
    private var visitFileFailed: ((Path, IOException) -> FileVisitResult)? = null
    private var postVisitDirectory: ((Path, IOException?) -> FileVisitResult)? = null
    private var isBuilt: Boolean = false

    override fun preVisitDirectory(function: (directory: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(preVisitDirectory, "preVisitDirectory")
        preVisitDirectory = function
    }

    override fun visitFile(function: (file: Path, attributes: BasicFileAttributes) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(visitFile, "visitFile")
        visitFile = function
    }

    override fun visitFileFailed(function: (file: Path, exception: IOException) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(visitFileFailed, "visitFileFailed")
        visitFileFailed = function
    }

    override fun postVisitDirectory(function: (directory: Path, exception: IOException?) -> FileVisitResult): Unit {
        checkIsNotBuilt()
        checkNotDefined(postVisitDirectory, "postVisitDirectory")
        postVisitDirectory = function
    }

    fun build(): FileVisitor<Path> {
        checkIsNotBuilt()
        isBuilt = true
        return FileVisitorImpl(preVisitDirectory, visitFile, visitFileFailed, postVisitDirectory)
    }

    private fun checkIsNotBuilt() {
        if (isBuilt) throw IllegalStateException("This builder was already built")
    }

    private fun checkNotDefined(function: Any?, name: String) {
        if (function != null) throw IllegalStateException("$name was already defined")
    }
}


private class FileVisitorImpl(
    private val preVisitDirectory: ((Path, BasicFileAttributes) -> FileVisitResult)?,
    private val visitFile: ((Path, BasicFileAttributes) -> FileVisitResult)?,
    private val visitFileFailed: ((Path, IOException) -> FileVisitResult)?,
    private val postVisitDirectory: ((Path, IOException?) -> FileVisitResult)?,
) : SimpleFileVisitor<Path>() {
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
        this.preVisitDirectory?.invoke(dir, attrs) ?: super.preVisitDirectory(dir, attrs)

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
        this.visitFile?.invoke(file, attrs) ?: super.visitFile(file, attrs)

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
        this.visitFileFailed?.invoke(file, exc) ?: super.visitFileFailed(file, exc)

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult =
        this.postVisitDirectory?.invoke(dir, exc) ?: super.postVisitDirectory(dir, exc)
}
