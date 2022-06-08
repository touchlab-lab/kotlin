/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes

internal class SecurePathTreeWalk private constructor(
    private val linkOptions: Array<LinkOption>,
    private var onFile: ((SecureDirectoryStream<Path>?, Path) -> Unit)?,
    private var onEnter: ((SecureDirectoryStream<Path>?, Path) -> Unit)?,
    private var onLeave: ((SecureDirectoryStream<Path>?, Path) -> Unit)?,
    private var onFail: ((f: Path, e: Exception) -> Unit)?,
) {
    constructor(followLinks: Boolean) : this(
        linkOptions = LinkFollowing.toOptions(followLinks),
        onFile = null,
        onEnter = null,
        onLeave = null,
        onFail = null
    )

    fun onFile(function: (SecureDirectoryStream<Path>?, Path) -> Unit): SecurePathTreeWalk {
        return this.apply { onFile = function }
    }

    fun onEnterDirectory(function: (SecureDirectoryStream<Path>?, Path) -> Unit): SecurePathTreeWalk {
        return this.apply { onEnter = function }
    }

    fun onLeaveDirectory(function: (SecureDirectoryStream<Path>?, Path) -> Unit): SecurePathTreeWalk {
        return this.apply { onLeave = function }
    }

    fun onFail(function: (Path, Exception) -> Unit): SecurePathTreeWalk {
        return this.apply { onFail = function }
    }

    private fun tryInvoke(
        function: ((SecureDirectoryStream<Path>?, Path) -> Unit)?,
        stream: SecureDirectoryStream<Path>?,
        path: Path
    ) {
        try {
            function?.invoke(stream, path)
        } catch (exception: Exception) {
            onFail?.invoke(path, exception) ?: throw exception
        }
    }

    private var stack: PathNode? = null

    private fun beforeWalkingEntries(stream: SecureDirectoryStream<Path>?, path: Path, key: Any?) {
        stack = PathNode(path, key, stack)
        if (stack!!.createsCycle())
            throw FileSystemLoopException(path.toString())

        tryInvoke(onEnter, stream, path)
    }

    private fun afterWalkingEntries(stream: SecureDirectoryStream<Path>?, path: Path) {
        tryInvoke(onLeave, stream, path)
        stack = stack!!.parent
    }

    fun walk(path: Path): Unit {
        // test parent is symlink
        // test path is symlink
        // test path contains symlink in the middle
        // test parent doesn't exist
        // test path doesn't exist

        // TODO: https://github.com/google/guava/blob/78d67c94dbe95b918c839b3a0b50d44508c2838b/guava/src/com/google/common/io/MoreFiles.java#L722
        //       Does it make sense in our case?

        var useInsecure = true

        path.parent?.let { parent ->
            val directoryStream = try { Files.newDirectoryStream(parent) } catch (exception: Throwable) { null }
            directoryStream?.use { stream ->
                if (stream is SecureDirectoryStream<Path>) {
                    useInsecure = false
                    stream.handleEntry(path.relativeTo(parent))
                }
            }
        }

        if (useInsecure) {
            insecureHandleEntry(path)
        }
    }

    // secure walk

    private fun SecureDirectoryStream<Path>.walkEntries() {
        forEach { handleEntry(it) }
    }

    private fun SecureDirectoryStream<Path>.handleEntry(entry: Path) {
        val attributes = directoryAttributesOrNull(entry)

        if (attributes != null) {
            enterDirectory(entry, attributes.fileKey())
        } else {
            tryInvoke(onFile, this, entry)
        }
    }

    private fun SecureDirectoryStream<Path>.enterDirectory(path: Path, key: Any?) {
        beforeWalkingEntries(this, path, key)

        try {
            this.newDirectoryStream(path).use { it.walkEntries() }
        } catch (exception: Exception) {
            onFail?.invoke(path, exception)
        }

        afterWalkingEntries(this, path)
    }

    /** If the given [path] is a directory, returns its attributes. Returns `null` otherwise. */
    private fun SecureDirectoryStream<Path>.directoryAttributesOrNull(path: Path): BasicFileAttributes? {
        return try {
            getFileAttributeView(path, BasicFileAttributeView::class.java, *linkOptions).readAttributes().takeIf { it.isDirectory }
        } catch (exception: Exception) {
            // ignore
            null
        }
    }

    // insecure walk

    private fun DirectoryStream<Path>.insecureWalkEntries() {
        forEach { insecureHandleEntry(it) }
    }

    private fun insecureHandleEntry(entry: Path) {
        val attributes = insecureDirectoryAttributesOrNull(entry)

        if (attributes != null) {
            insecureEnterDirectory(entry, attributes.fileKey())
        } else {
            tryInvoke(onFile, null, entry)
        }
    }

    private fun insecureEnterDirectory(path: Path, key: Any?) {
        beforeWalkingEntries(null, path, key)

        try {
            Files.newDirectoryStream(path).use { it.insecureWalkEntries() }
        } catch (exception: Exception) {
            onFail?.invoke(path, exception)
        }

        afterWalkingEntries(null, path)
    }

    /** If the given [path] is a directory, returns its attributes. Returns `null` otherwise. */
    private fun insecureDirectoryAttributesOrNull(path: Path): BasicFileAttributes? {
        return try {
            path.readAttributes<BasicFileAttributes>(*linkOptions).takeIf { it.isDirectory }
        } catch (exception: Exception) {
            // ignore
            null
        }
    }
}
