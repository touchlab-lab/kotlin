/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.CapturedTypeConstructorMarker
import org.jetbrains.kotlin.types.typeUtil.builtIns

interface CapturedTypeConstructor : CapturedTypeConstructorMarker, TypeConstructor {
    val projection: TypeProjection
}

class NewCapturedTypeConstructor(
    override val projection: TypeProjection,
    private var supertypesComputation: (() -> List<UnwrappedType>)? = null,
    private val original: NewCapturedTypeConstructor? = null,
    val typeParameter: TypeParameterDescriptor? = null
) : CapturedTypeConstructor {

    constructor(
        projection: TypeProjection,
        supertypes: List<UnwrappedType>,
        original: NewCapturedTypeConstructor? = null
    ) : this(projection, { supertypes }, original)

    private val _supertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        supertypesComputation?.invoke()
    }

    fun initializeSupertypes(supertypes: List<UnwrappedType>) {
        assert(this.supertypesComputation == null) {
            "Already initialized! oldValue = ${this.supertypesComputation}, newValue = $supertypes"
        }
        this.supertypesComputation = { supertypes }
    }

    override fun getSupertypes() = _supertypes ?: emptyList()
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun isFinal() = false
    override fun isDenotable() = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null
    override fun getBuiltIns(): KotlinBuiltIns = projection.type.builtIns

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) =
        NewCapturedTypeConstructor(
            projection.refine(kotlinTypeRefiner),
            supertypesComputation?.let {
                {
                    supertypes.map { it.refine(kotlinTypeRefiner) }
                }
            },
            original ?: this,
            typeParameter = typeParameter
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewCapturedTypeConstructor

        return (original ?: this) === (other.original ?: other)
    }

    override fun hashCode(): Int = original?.hashCode() ?: super.hashCode()
    override fun toString() = "CapturedType($projection)"
}
