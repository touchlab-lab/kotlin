/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_MACOSX || KONAN_IOS || KONAN_TVOS || KONAN_WATCHOS

#if defined(__has_feature) && __has_feature(objc_arc)
#error "Assumes that ARC is not used"
#endif

#include <functional>
#include <utility>

#include "ObjCForward.hpp"

OBJC_FORWARD_DECLARE(NSObject);

namespace kotlin::objc_support {

namespace internal {

class ObjectPtrImpl {
public:
    ObjectPtrImpl() noexcept;
    explicit ObjectPtrImpl(NSObject* object) noexcept;

    ObjectPtrImpl(const ObjectPtrImpl& rhs) noexcept;
    ObjectPtrImpl(ObjectPtrImpl&& rhs) noexcept;

    ~ObjectPtrImpl();

    ObjectPtrImpl& operator=(const ObjectPtrImpl& rhs) noexcept {
        ObjectPtrImpl tmp(rhs);
        swap(tmp);
        return *this;
    }

    ObjectPtrImpl& operator=(ObjectPtrImpl&& rhs) noexcept {
        ObjectPtrImpl tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    void swap(ObjectPtrImpl& rhs) noexcept;

    NSObject* get() const noexcept;
    bool valid() const noexcept;

    void reset() noexcept;
    void reset(NSObject* object) noexcept;

    bool operator==(const ObjectPtrImpl& rhs) const noexcept;
    bool operator<(const ObjectPtrImpl& rhs) const noexcept;

    std::size_t computeHash() const noexcept;

private:
    NSObject* object_;
};

} // namespace internal

template <typename T>
class object_ptr {
public:
    object_ptr() noexcept = default;
    explicit object_ptr(T* ptr) noexcept : impl_(ptr) {}

    void swap(object_ptr<T>& rhs) noexcept { impl_.swap(rhs.impl_); }

    T* get() const noexcept { return (T*)impl_.get(); }
    T* operator*() const noexcept { return (T*)impl_.get(); }

    explicit operator bool() const noexcept { return impl_.valid(); }

    void reset() noexcept { impl_.reset(); }
    void reset(T* ptr) noexcept { impl_.reset(ptr); }

    template <typename U>
    bool operator==(const object_ptr<U>& rhs) const noexcept {
        return impl_ == rhs.impl_;
    }

    template <typename U>
    bool operator<(const object_ptr<U>& rhs) const noexcept {
        return impl_ < rhs.impl_;
    }

private:
    friend struct std::hash<object_ptr>;

    internal::ObjectPtrImpl impl_;
};

template <typename T, typename U>
bool operator!=(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return !(lhs == rhs);
}

template <typename T, typename U>
bool operator>(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return rhs < lhs;
}

template <typename T, typename U>
bool operator<=(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return !(lhs > rhs);
}

template <typename T, typename U>
bool operator>=(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return !(lhs < rhs);
}

} // namespace kotlin::objc_support

namespace std {

template <typename T>
void swap(kotlin::objc_support::object_ptr<T>& lhs, kotlin::objc_support::object_ptr<T>& rhs) noexcept {
    lhs.swap(rhs);
}

template <typename T>
struct hash<kotlin::objc_support::object_ptr<T>> {
    std::size_t operator()(const kotlin::objc_support::object_ptr<T>& value) { return value.impl_.computeHash(); }
};

// TODO: std::atomic specialization? Can it just be a blanket specialization?

} // namespace std

#endif
