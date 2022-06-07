/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.K2PluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class ParcelizeComponentRegistrar : K2PluginRegistrar() {
    companion object {
        fun registerParcelizeComponents(extensionStorage: ExtensionStorage) = with(extensionStorage) {
            ExpressionCodegenExtension.registerExtension(ParcelizeCodegenExtension())
            IrGenerationExtension.registerExtension(ParcelizeIrGeneratorExtension())
            SyntheticResolveExtension.registerExtension(ParcelizeResolveExtension())
            ClassBuilderInterceptorExtension.registerExtension(ParcelizeClinitClassBuilderInterceptorExtension())
            StorageComponentContainerContributor.registerExtension(ParcelizeDeclarationCheckerComponentContainerContributor())
        }
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        registerParcelizeComponents(this)
    }

    override val supportsK2: Boolean
        get() = true
}

class ParcelizeDeclarationCheckerComponentContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        if (platform.isJvm()) {
            container.useInstance(ParcelizeDeclarationChecker())
            container.useInstance(ParcelizeAnnotationChecker())
        }
    }
}
