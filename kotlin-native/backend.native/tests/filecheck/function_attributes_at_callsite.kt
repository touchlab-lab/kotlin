/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK: declare void @ThrowException(%struct.ObjHeader*) #[[THROW_EXCEPTION_ATTRIBUTES:[0-9]+]]

// CHECK: void @"kfun:#flameThrower(){}kotlin.Nothing"() #[[FLAME_THROWER_DECLARATION_ATTRIBUTES:[0-9]+]]
fun flameThrower(): Nothing {
    // CHECK: invoke void @ThrowException(%struct.ObjHeader* {{.*}}) #[[THROW_EXCEPTION_ATTRIBUTES]]
    throw Throwable("🔥")
}

// CHECK-LABEL: void @"kfun:#main(){}"()
fun main() {
    // CHECK: call void @"kfun:#flameThrower(){}kotlin.Nothing"() #[[FLAME_THROWER_CALLSITE_ATTRIBUTES:[0-9]+]]
    flameThrower()
}

// CHECK: attributes #[[FLAME_THROWER_DECLARATION_ATTRIBUTES]] = { noreturn {{.+}} }
// CHECK: attributes #[[FLAME_THROWER_CALLSITE_ATTRIBUTES]] = { noreturn }