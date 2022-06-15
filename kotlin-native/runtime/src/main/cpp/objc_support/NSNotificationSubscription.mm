/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_MACOSX || KONAN_IOS || KONAN_TVOS || KONAN_WATCHOS

#include "NSNotificationSubscription.hpp"

#import <Foundation/NSNotification.h>

#if defined(__has_feature) && __has_feature(objc_arc)
#error "Assumes that ARC is not used"
#endif

using namespace kotlin;

@interface Kotlin_objc_support_NSNotificationSubscriptionImpl : NSObject {
    NSNotificationCenter* center_;
    std::function<void()> handler_;
}

- (instancetype)initWithNotificationCenter:(NSNotificationCenter*)center
                                      name:(NSNotificationName)name
                                   handler:(std::function<void()>)handler;

- (void)reset;

- (void)onNotification:(NSNotification*)notification;

@end

@implementation Kotlin_objc_support_NSNotificationSubscriptionImpl

- (instancetype)initWithNotificationCenter:(NSNotificationCenter*)center
                                      name:(NSNotificationName)name
                                   handler:(std::function<void()>)handler {
    if ((self = [super init])) {
        center_ = center;
        handler_ = std::move(handler);

        [center_ addObserver:self selector:@selector(onNotification:) name:name object:nil];
    }
    return self;
}

- (void)reset {
    [center_ removeObserver:self];
}

- (void)onNotification:(NSNotification*)notification {
    handler_();
}

@end

objc_support::NSNotificationSubscription::NSNotificationSubscription(
        NSNotificationCenter* center, NSString* name, std::function<void()> handler) noexcept :
    impl_([[Kotlin_objc_support_NSNotificationSubscriptionImpl alloc] initWithNotificationCenter:center
                                                                                            name:name
                                                                                         handler:std::move(handler)]) {}

objc_support::NSNotificationSubscription::NSNotificationSubscription(NSString* name, std::function<void()> handler) noexcept :
    impl_([[Kotlin_objc_support_NSNotificationSubscriptionImpl alloc] initWithNotificationCenter:[NSNotificationCenter defaultCenter]
                                                                                            name:name
                                                                                         handler:std::move(handler)]) {}

void objc_support::NSNotificationSubscription::reset() noexcept {
    @autoreleasepool {
        [*impl_ reset];
        impl_.reset();
    }
}

#endif
