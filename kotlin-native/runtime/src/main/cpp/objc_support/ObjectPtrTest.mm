/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_MACOSX || KONAN_IOS || KONAN_TVOS || KONAN_WATCHOS

#include "ObjectPtr.hpp"

#include <functional>
#include <objc/NSObject.h>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "std_support/Memory.hpp"
#include "Utils.hpp"

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

@interface ObjectPtrTestClass : NSObject {
    std_support::unique_ptr<WithDestructorHook> contents_;
}

@property(readonly) WithDestructorHook* contents;

- (instancetype)initWithDestructorHook:(std::function<DestructorHook>)hook;

@end

@implementation ObjectPtrTestClass

- (instancetype)initWithDestructorHook:(std::function<DestructorHook>)hook {
    if ((self = [super init])) {
        contents_ = std_support::make_unique<WithDestructorHook>(hook);
    }
    return self;
}

- (WithDestructorHook*)contents {
    return contents_.get();
}

@end

TEST(ObjectPtrTest, DefaultCtor) {
    objc_support::object_ptr<ObjectPtrTestClass> obj;
    EXPECT_FALSE(obj);
    EXPECT_THAT(*obj, nil);
    EXPECT_THAT(obj.get(), nil);
}

TEST(ObjectPtrTest, ObjectCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<ObjectPtrTestClass> obj(ptr);
        EXPECT_TRUE(obj);
        EXPECT_THAT(*obj, ptr);
        EXPECT_THAT(obj.get(), ptr);
        EXPECT_CALL(destructorHook, Call(ptr.contents));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, CopyCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<ObjectPtrTestClass> obj1(ptr);
        objc_support::object_ptr<ObjectPtrTestClass> obj2(obj1);
        EXPECT_TRUE(obj1);
        EXPECT_THAT(*obj1, ptr);
        EXPECT_THAT(obj1.get(), ptr);
        EXPECT_TRUE(obj2);
        EXPECT_THAT(*obj2, ptr);
        EXPECT_THAT(obj2.get(), ptr);

        EXPECT_CALL(destructorHook, Call(ptr.contents)).Times(0);
        obj1.reset();
        testing::Mock::VerifyAndClearExpectations(&destructorHook);
        EXPECT_FALSE(obj1);
        EXPECT_THAT(*obj1, nil);
        EXPECT_THAT(obj1.get(), nil);
        EXPECT_TRUE(obj2);
        EXPECT_THAT(*obj2, ptr);
        EXPECT_THAT(obj2.get(), ptr);

        EXPECT_CALL(destructorHook, Call(ptr.contents));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, MoveCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<ObjectPtrTestClass> obj1(ptr);

        EXPECT_CALL(destructorHook, Call(ptr.contents)).Times(0);
        objc_support::object_ptr<ObjectPtrTestClass> obj2(std::move(obj1));
        testing::Mock::VerifyAndClearExpectations(&destructorHook);
        EXPECT_FALSE(obj1);
        EXPECT_THAT(*obj1, nil);
        EXPECT_THAT(obj1.get(), nil);
        EXPECT_TRUE(obj2);
        EXPECT_THAT(*obj2, ptr);
        EXPECT_THAT(obj2.get(), ptr);

        EXPECT_CALL(destructorHook, Call(ptr.contents));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, CopyAssignmentIntoEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<ObjectPtrTestClass> obj2;
        {
            objc_support::object_ptr<ObjectPtrTestClass> obj1(ptr);
            obj2 = obj1;
            EXPECT_TRUE(obj1);
            EXPECT_THAT(*obj1, ptr);
            EXPECT_THAT(obj1.get(), ptr);
            EXPECT_TRUE(obj2);
            EXPECT_THAT(*obj2, ptr);
            EXPECT_THAT(obj2.get(), ptr);

            EXPECT_CALL(destructorHook, Call(ptr.contents)).Times(0);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_TRUE(obj2);
        EXPECT_THAT(*obj2, ptr);
        EXPECT_THAT(obj2.get(), ptr);

        EXPECT_CALL(destructorHook, Call(ptr.contents));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, CopyAssignmentIntoSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<ObjectPtrTestClass> obj2(ptr2);
        {
            objc_support::object_ptr<ObjectPtrTestClass> obj1(ptr1);

            EXPECT_CALL(destructorHook, Call(ptr2.contents));
            obj2 = obj1;
            testing::Mock::VerifyAndClearExpectations(&destructorHook);

            EXPECT_TRUE(obj1);
            EXPECT_THAT(*obj1, ptr1);
            EXPECT_THAT(obj1.get(), ptr1);
            EXPECT_TRUE(obj2);
            EXPECT_THAT(*obj2, ptr1);
            EXPECT_THAT(obj2.get(), ptr1);

            EXPECT_CALL(destructorHook, Call(ptr1.contents)).Times(0);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_TRUE(obj2);
        EXPECT_THAT(*obj2, ptr1);
        EXPECT_THAT(obj2.get(), ptr1);

        EXPECT_CALL(destructorHook, Call(ptr1.contents));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, MoveAssignmentIntoEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<ObjectPtrTestClass> obj2;
        {
            EXPECT_CALL(destructorHook, Call(ptr.contents)).Times(0);

            objc_support::object_ptr<ObjectPtrTestClass> obj1(ptr);
            obj2 = std::move(obj1);
            EXPECT_FALSE(obj1);
            EXPECT_THAT(*obj1, nil);
            EXPECT_THAT(obj1.get(), nil);
            EXPECT_TRUE(obj2);
            EXPECT_THAT(*obj2, ptr);
            EXPECT_THAT(obj2.get(), ptr);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_TRUE(obj2);
        EXPECT_THAT(*obj2, ptr);
        EXPECT_THAT(obj2.get(), ptr);

        EXPECT_CALL(destructorHook, Call(ptr.contents));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TEST(ObjectPtrTest, MoveAssignmentIntoSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    auto* ptr2 = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    {
        objc_support::object_ptr<ObjectPtrTestClass> obj2(ptr2);
        {
            objc_support::object_ptr<ObjectPtrTestClass> obj1(ptr1);

            EXPECT_CALL(destructorHook, Call(ptr2.contents));
            EXPECT_CALL(destructorHook, Call(ptr1.contents)).Times(0);
            obj2 = std::move(obj1);
            testing::Mock::VerifyAndClearExpectations(&destructorHook);

            EXPECT_FALSE(obj1);
            EXPECT_THAT(*obj1, nil);
            EXPECT_THAT(obj1.get(), nil);
            EXPECT_TRUE(obj2);
            EXPECT_THAT(*obj2, ptr1);
            EXPECT_THAT(obj2.get(), ptr1);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_TRUE(obj2);
        EXPECT_THAT(*obj2, ptr1);
        EXPECT_THAT(obj2.get(), ptr1);

        EXPECT_CALL(destructorHook, Call(ptr1.contents));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

// TODO: swap

TEST(ObjectPtrTest, ResetEmptyFromEmpty) {
    objc_support::object_ptr<ObjectPtrTestClass> obj;
    obj.reset();
    EXPECT_FALSE(obj);
    EXPECT_THAT(*obj, nil);
    EXPECT_THAT(obj.get(), nil);
}

TEST(ObjectPtrTest, ResetEmptyFromSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = [[ObjectPtrTestClass alloc] initWithDestructorHook:destructorHook.AsStdFunction()];
    objc_support::object_ptr<ObjectPtrTestClass> obj(ptr);

    EXPECT_CALL(destructorHook, Call(ptr.contents));
    obj.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    EXPECT_FALSE(obj);
    EXPECT_THAT(*obj, nil);
    EXPECT_THAT(obj.get(), nil);
}

// TODO: reset object
// TODO: comparisons
// TODO: hashing

#endif
