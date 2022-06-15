/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_MACOSX || KONAN_IOS || KONAN_TVOS || KONAN_WATCHOS

#include <functional>

#include "ObjCForward.hpp"
#include "ObjectPtr.hpp"
#include "Utils.hpp"

OBJC_FORWARD_DECLARE(NSNotificationCenter);
OBJC_FORWARD_DECLARE(NSString);
OBJC_FORWARD_DECLARE(Kotlin_objc_support_NSNotificationSubscriptionImpl);

namespace kotlin::objc_support {

class NSNotificationSubscription : private MoveOnly {
public:
    NSNotificationSubscription(NSNotificationCenter* center, NSString* name, std::function<void()> handler) noexcept;
    NSNotificationSubscription(NSString* name, std::function<void()> handler) noexcept;

    NSNotificationSubscription(NSNotificationSubscription&&) = default;
    NSNotificationSubscription& operator=(NSNotificationSubscription&&) = default;

    void swap(NSNotificationSubscription& rhs) noexcept {
        impl_.swap(rhs.impl_);
    }

    ~NSNotificationSubscription() { reset(); }

    void reset() noexcept;
    bool subscribed() const noexcept { return static_cast<bool>(impl_); }
    explicit operator bool() const noexcept { return subscribed(); }

private:
   object_ptr<Kotlin_objc_support_NSNotificationSubscriptionImpl> impl_;
};

} // namespace kotlin::objc_support

#endif
