/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ClassSymbolHashCalculator {
    private val classSymbolHashes = mutableMapOf<IdSignature, ICHash>()

    val symbolHashes: Map<IdSignature, ICHash>
        get() = classSymbolHashes

    fun updateClassSymbolHashes(fragments: Collection<IrModuleFragment>) {
        fragments.forEach {
            it.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.symbol.signature?.let { signature ->
                        if (signature !in classSymbolHashes) {
                            classSymbolHashes[signature] = declaration.irClassSymbolHashForIC()
                        }
                    }
                    super.visitClass(declaration)
                }
            })
        }
    }
}
