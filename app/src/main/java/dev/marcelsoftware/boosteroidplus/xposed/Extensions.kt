package dev.marcelsoftware.boosteroidplus.xposed

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.luckypray.dexkit.result.MethodData

fun MethodData.hook(
    classLoader: ClassLoader,
    block: Hook.() -> Unit,
) {
    val hook = Hook()
    hook.block()
    XposedBridge.hookMethod(
        this.getMethodInstance(classLoader),
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                hook.beforeCallback?.invoke(param)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                hook.afterCallback?.invoke(param)
            }
        },
    )
}

fun hookMethod(
    clazz: Class<*>,
    methodName: String,
    vararg parameterTypes: Any,
    block: Hook.() -> Unit,
) {
    val hook = Hook()
    hook.block()

    XposedHelpers.findAndHookMethod(
        clazz,
        methodName,
        *parameterTypes,
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                hook.beforeCallback?.invoke(param)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                hook.afterCallback?.invoke(param)
            }
        },
    )
}

fun hookMethod(
    className: String,
    classLoader: ClassLoader,
    methodName: String,
    vararg parameterTypes: Any,
    block: Hook.() -> Unit,
) {
    val hook = Hook()
    hook.block()

    XposedHelpers.findAndHookMethod(
        className,
        classLoader,
        methodName,
        *parameterTypes,
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                hook.beforeCallback?.invoke(param)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                hook.afterCallback?.invoke(param)
            }
        },
    )
}

class Hook {
    var beforeCallback: ((XC_MethodHook.MethodHookParam) -> Unit)? = null
    var afterCallback: ((XC_MethodHook.MethodHookParam) -> Unit)? = null

    fun before(callback: (XC_MethodHook.MethodHookParam) -> Unit) {
        beforeCallback = callback
    }

    fun after(callback: (XC_MethodHook.MethodHookParam) -> Unit) {
        afterCallback = callback
    }
}
