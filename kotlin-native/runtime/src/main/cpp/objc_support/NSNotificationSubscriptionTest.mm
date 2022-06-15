/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_MACOSX || KONAN_IOS || KONAN_TVOS || KONAN_WATCHOS

#include "NSNotificationSubscription.hpp"

#import <Foundation/NSNotification.h>
#import <Foundation/NSString.h>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "std_support/Memory.hpp"

using namespace kotlin;

namespace {

class WithDestructorHook;

using DestructorHook = void(WithDestructorHook*);

class WithDestructorHook : private Pinned {
public:
    explicit WithDestructorHook(std::function<DestructorHook> hook) : hook_(std::move(hook)) {}

    ~WithDestructorHook() { hook_(this); }

private:
    std::function<DestructorHook> hook_;
};

} // namespace

class NSNotificationSubscriptionTest : public testing::Test {
public:
    objc_support::NSNotificationSubscription subscribe(const char* name, std::function<void()> handler) noexcept {
        return objc_support::NSNotificationSubscription(center_, [NSString stringWithUTF8String:name], std::move(handler));
    }

    void post(const char* name) noexcept { [center_ postNotificationName:[NSString stringWithUTF8String:name] object:nil]; }

private:
    NSNotificationCenter* center_ = [[NSNotificationCenter alloc] init];
};

TEST_F(NSNotificationSubscriptionTest, Subscribed) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());
    EXPECT_TRUE(subscription.subscribed());
    EXPECT_TRUE(subscription);

    subscription.reset();
    EXPECT_FALSE(subscription.subscribed());
    EXPECT_FALSE(subscription);
}

TEST_F(NSNotificationSubscriptionTest, Post) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());

    EXPECT_CALL(handler, Call());
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, PostWrongName) {
    constexpr const char* name = "NOTIFICATION_NAME";
    constexpr const char* wrongName = "NOTIFICATION_NAME_WRONG";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());

    EXPECT_CALL(handler, Call()).Times(0);
    post(wrongName);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, PostAfterReset) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    auto subscription = subscribe(name, handler.AsStdFunction());
    subscription.reset();

    EXPECT_CALL(handler, Call()).Times(0);
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, PostAfterDtor) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<void()>> handler;

    {
        // Create and destroy subscription object.
        subscribe(name, handler.AsStdFunction());
    }

    EXPECT_CALL(handler, Call()).Times(0);
    post(name);
    testing::Mock::VerifyAndClearExpectations(&handler);
}

TEST_F(NSNotificationSubscriptionTest, DestroyHandlerAfterReset) {
    constexpr const char* name = "NOTIFICATION_NAME";
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;
    auto targetOwner = std_support::make_shared<WithDestructorHook>(destructorHook.AsStdFunction());
    auto* target = targetOwner.get();

    auto subscription = subscribe(name, [targetOwner] { /* nothing to do */ });
    targetOwner.reset();
    // Only `subscription` owns `targetOwner` now.

    EXPECT_CALL(destructorHook, Call(target));
    subscription.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

// TODO: destruction of handler.
// TODO: move semantics.

#endif
