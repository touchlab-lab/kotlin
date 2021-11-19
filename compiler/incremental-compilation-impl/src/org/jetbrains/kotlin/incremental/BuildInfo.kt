/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.AbiSnapshotImpl.Companion.readAbiSnapshot
import org.jetbrains.kotlin.incremental.AbiSnapshotImpl.Companion.writeAbiSnapshot
import org.jetbrains.kotlin.incremental.JarSnapshotImpl.Companion.readJarSnapshot
import org.jetbrains.kotlin.incremental.JarSnapshotImpl.Companion.writeJarSnapshot
import java.io.*

data class BuildInfo(
    val startTS: Long,
    val dependencyToAbiSnapshot: Map<String, AbiSnapshot> = mapOf(),
    val dependencyToJarSnapshot: Map<String, JarSnapshot> = mapOf()
) : Serializable {
    companion object {
        private fun ObjectInputStream.readBuildInfo() : BuildInfo {
            val ts = readLong()
            val abiSnapshots = readAbiSnapshots()
            val jarSnapshots = readJarSnapshots()
            return BuildInfo(ts, abiSnapshots, jarSnapshots)
        }

        private fun ObjectInputStream.readJarSnapshots(): HashMap<String, JarSnapshot> {
            val size = readInt()
            val jarSnapshots = HashMap<String, JarSnapshot>(size)
            repeat(size) {
                val jarPath = readUTF()
                val snapshot = readJarSnapshot()
                jarSnapshots[jarPath] = snapshot
            }
            return jarSnapshots
        }

        private fun ObjectInputStream.readAbiSnapshots(): HashMap<String, AbiSnapshot> {
            val size = readInt()
            val abiSnapshots = HashMap<String, AbiSnapshot>(size)
            repeat(size) {
                val identifier = readUTF()
                val snapshot = readAbiSnapshot()
                abiSnapshots[identifier] = snapshot
            }
            return abiSnapshots
        }

        private fun ObjectOutputStream.writeBuildInfo(buildInfo: BuildInfo) {
            writeLong(buildInfo.startTS)
            writeInt(buildInfo.dependencyToAbiSnapshot.size)
            for((identifier, abiSnapshot) in buildInfo.dependencyToAbiSnapshot) {
                writeUTF(identifier)
                writeAbiSnapshot(abiSnapshot)
            }
            writeInt(buildInfo.dependencyToJarSnapshot.size)
            for ((file, lookups) in buildInfo.dependencyToJarSnapshot) {
                writeUTF(file)
                writeJarSnapshot(lookups)
            }
        }

        fun read(file: File): BuildInfo? =
            try {
                ObjectInputStream(FileInputStream(file)).use {
                    it.readBuildInfo()
                }
            } catch (e: Exception) {
                null
            }

        fun write(buildInfo: BuildInfo, file: File) {
            ObjectOutputStream(FileOutputStream(file)).use {
                it.writeBuildInfo(buildInfo)
            }
        }
    }
}