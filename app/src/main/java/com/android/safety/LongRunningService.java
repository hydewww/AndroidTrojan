package com.android.safety;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.TimeZone;

public class LongRunningService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    public  boolean changeAirPlane(boolean enable) {
        Process process = null;
        DataOutputStream os = null;
        String cmd="settings put global airplane_mode_on ";
        String cmd2="am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ";
        try {
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            if (enable) {
                os.writeBytes(cmd + "1\n");
                os.writeBytes(cmd2 + "true\n");
            } else {
                os.writeBytes(cmd + "0\n");
                os.writeBytes(cmd2 + "false\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }

    private void setAirPlaneModeByRadio(boolean enable) {
        try {
            Class<?> telephonyManager = Class.forName("android.telephony.TelephonyManager");
            Method setRadio = telephonyManager.getMethod("setRadio", boolean.class);
            setRadio.invoke(telephonyManager.getMethod("getDefault").invoke(null), enable);
            if (enable) {
                Method toggleRadioOnOff = telephonyManager.getMethod("toggleRadioOnOff");
                toggleRadioOnOff.invoke(telephonyManager.getMethod("getDefault").invoke(null));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                Integer minute = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.MINUTE);
//                changeAirPlane(minute % 2 == 1);
                Integer second = Calendar.getInstance(TimeZone.getTimeZone("GMT+8:00")).get(Calendar.SECOND);
                changeAirPlane(second < 30);

//                if (minute % 2 == 1) { // 时间内则禁用
//                } else {
//
//                }
            }
        }).start();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int minute = 60 * 1000;
        long triggerAtTime = System.currentTimeMillis() - System.currentTimeMillis() % minute + minute / 2;
        Intent i = new Intent(this, LongRunningService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.setExact(AlarmManager.RTC_WAKEUP, triggerAtTime, pi);
        Toast.makeText(this, "LTS", Toast.LENGTH_SHORT).show();

        return super.onStartCommand(intent, flags, startId);
    }
}
