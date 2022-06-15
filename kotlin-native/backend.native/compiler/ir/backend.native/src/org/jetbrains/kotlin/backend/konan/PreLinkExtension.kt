/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

interface PreLinkExtension {
    companion object :
        ProjectExtensionDescriptor<PreLinkExtension>(
            "org.jetbrains.kotlin.preLinkExtension", PreLinkExtension::class.java
        )

    fun process(context: PreLinkContext): PreLinkResult?
}

data class PreLinkResult(
    val additionalObjectFiles: List<ObjectFile>,
    val additionalLinkerFlags: List<String>,
)

interface PreLinkContext {
    val config: KonanConfig
    val backendContext: CommonBackendContext
    val objcExportNamer: ObjCExportNamer
}

internal class NativePreLinkContext(
    override val config: KonanConfig,
    override val backendContext: CommonBackendContext,
    override val objcExportNamer: ObjCExportNamer,
) : PreLinkContext
