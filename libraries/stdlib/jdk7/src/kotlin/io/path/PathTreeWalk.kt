/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * This class is intended to implement different file traversal methods.
 * It allows to iterate through all files inside a given directory.
 * The order in which sibling files are visited is unspecified.
 *
 * If the file located by this path is not a directory, the walker iterates only it.
 * If the file located by this path does not exist, the walker iterates nothing, i.e. it's equivalent to an empty sequence.
 */
@ExperimentalStdlibApi
internal class PathTreeWalk(
    private val start: Path,
    private val options: Array<out PathWalkOption>
) : Sequence<Path> {

    private val linkOptions: Array<LinkOption>
        get() = LinkFollowing.toOptions(followLinks = options.contains(PathWalkOption.FOLLOW_LINKS))

    private val includeDirectories: Boolean
        get() = options.contains(PathWalkOption.INCLUDE_DIRECTORIES)

    private val isBFS: Boolean
        get() = options.contains(PathWalkOption.BREADTH_FIRST)

    override fun iterator(): Iterator<Path> = if (isBFS) bfsIterator() else dfsIterator()

    private suspend inline fun SequenceScope<Path>.yieldIfNeeded(node: PathNode, entriesAction: (List<Path>) -> Unit) {
        val path = node.path
        if (path.isDirectory(*linkOptions)) {
            if (node.createsCycle())
                throw FileSystemLoopException(path.toString())

            if (includeDirectories)
                yield(path)

            if (path.isDirectory(*linkOptions)) // make sure the path was not deleted after it was yielded
                entriesAction(path.listDirectoryEntries())
        } else if (path.exists(LinkOption.NOFOLLOW_LINKS)) {
            yield(path)
        }
    }

    private fun dfsIterator() = iterator<Path> {
        // Stack of directory iterators, beginning from the start directory
        val stack = ArrayDeque<PathNode>()

        val startNode = PathNode(start, keyOf(start, linkOptions), null)
        yieldIfNeeded(startNode) {
            startNode.contentIterator = it.iterator()
            stack.addLast(startNode)
        }

        while (stack.isNotEmpty()) {
            val topNode = stack.last()
            val topIterator = topNode.contentIterator!!

            if (topIterator.hasNext()) {
                val path = topIterator.next()
                val pathNode = PathNode(path, keyOf(path, linkOptions), topNode)
                yieldIfNeeded(pathNode) {
                    pathNode.contentIterator = it.iterator()
                    stack.addLast(pathNode)
                }
            } else {
                // There is nothing more on the top of the stack, go back
                stack.removeLast()
            }
        }
    }

    private fun bfsIterator() = iterator<Path> {
        // Queue of entries to be visited.
        val queue = ArrayDeque<PathNode>()
        queue.addLast(PathNode(start, keyOf(start, linkOptions), null))

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            yieldIfNeeded(node) { entries ->
                entries.forEach {
                    queue.addLast(PathNode(it, keyOf(it, linkOptions), node))
                }
            }
        }
    }
}

private fun keyOf(path: Path, linkOptions: Array<LinkOption>): Any? {
    return try {
        path.readAttributes<BasicFileAttributes>(*linkOptions).fileKey()
    } catch (exception: Throwable) {
        null
    }
}

private fun PathNode.createsCycle(): Boolean {
    var ancestor = parent
    while (ancestor != null) {
        if (ancestor.key != null && key != null) {
            if (ancestor.key == key)
                return true
        } else {
            try {
                if (ancestor.path.isSameFileAs(path))
                    return true
            } catch (_: IOException) { // ignore
            } catch (_: SecurityException) { // ignore
            }
        }
        ancestor = ancestor.parent
    }

    return false
}

private class PathNode(val path: Path, val key: Any?, val parent: PathNode?) {
    var contentIterator: Iterator<Path>? = null
}

internal object LinkFollowing {
    private val nofollow = arrayOf(LinkOption.NOFOLLOW_LINKS)
    private val follow = emptyArray<LinkOption>()

    fun toOptions(followLinks: Boolean): Array<LinkOption> = if (followLinks) follow else nofollow
}
