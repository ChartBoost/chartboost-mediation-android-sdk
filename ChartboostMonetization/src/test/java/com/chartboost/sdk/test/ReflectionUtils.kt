package com.chartboost.sdk.test

import kotlin.reflect.KClass

inline fun <reified T> T.setPrivateField(
    field: String,
    value: Any,
): T =
    apply {
        T::class.java.declaredFields
            .first { it.name == field }
            .apply { isAccessible = true }
            .set(this, value)
    }

inline fun <reified T, R> T.getPrivateField(field: String): R? =
    T::class.java.declaredFields
        .first { it.name == field }
        .apply { isAccessible = true }
        .get(this) as R?

// Be careful when passing mocked arguments here, as they can have an unexpected class type and throwing
// a NoSuchMethodException. In this case consider using the overload with explicit types.
inline fun <reified T, R> T.callPrivateMethod(
    name: String,
    vararg args: Any,
): R? =
    T::class.java.getDeclaredMethod(name, *args.map { it::class.java }.toTypedArray())
        .apply { isAccessible = true }
        .invoke(this, *args) as R?

// Explicitly declares the types of the arguments and their values to avoid NoSuchMethodException.
inline fun <reified T, R> T.callPrivateMethod(
    name: String,
    args: List<Pair<KClass<*>, Any>>,
): R? =
    T::class.java.getDeclaredMethod(name, *args.map { it.first.java }.toTypedArray())
        .apply { isAccessible = true }
        .invoke(this, *args.map { it.second }.toTypedArray()) as R?
