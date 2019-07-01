package com.android.safety

import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Xposed : IXposedHookLoadPackage {
    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera", // 类名
            lpparam.classLoader,
            "open", // 方法名
            Int::class.java, // 参数，视方法而定
            object : XC_MethodReplacement(){ // 回调方法
                override fun replaceHookedMethod(param: MethodHookParam) { // 重写方法
                    XposedBridge.log("相机: " + lpparam.packageName)
                    val exception = java.lang.RuntimeException("")
                    param.result = exception // 将结果设为Runtime异常
                }
            })
    }

}