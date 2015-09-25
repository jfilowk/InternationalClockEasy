package com.estudio.xposeddemo;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Tutorial implements IXposedHookLoadPackage,IXposedHookZygoteInit {

    public static final String XPOSED = "XPOSED";
    public static final String SYSTEM_UI = "com.android.systemui";
    public static final String DESCLOCK = "com.android.deskclock";

    private HashMap<String, String> cityHour;
    XSharedPreferences prefs;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        List<String> listPackage = new ArrayList<>();
        listPackage.add(SYSTEM_UI);
        listPackage.add(DESCLOCK);

        cityHour = new HashMap<>();
        XposedBridge.log("Package _,-" + lpparam.packageName);

        if (!listPackage.contains(lpparam.packageName))
            return;

        XposedBridge.log("Estamos dentro de el paquete-> " + lpparam.packageName);
        if (lpparam.packageName.equals(DESCLOCK)) {


            XposedHelpers.findAndHookMethod("com.android.deskclock.worldclock.WorldClockAdapter", lpparam.classLoader, "reloadData", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Class clazz = XposedHelpers.findClass("com.android.deskclock.worldclock.WorldClockAdapter", lpparam.classLoader);
                    XposedBridge.log(Arrays.toString(clazz.getMethods()));
                    Field mClock = clazz.getDeclaredField("mCitiesList");
                    mClock.setAccessible(true);

                    SharedPreferences pref =
                            AndroidAppHelper.currentApplication().getSharedPreferences("user_settings", Context.MODE_WORLD_READABLE);
                    SharedPreferences.Editor editor = pref.edit();
                    Object[] mClocks = (Object[]) mClock.get(XposedHelpers.newInstance(clazz, AndroidAppHelper.currentApplication().getApplicationContext()));
                    for (Object clock : mClocks) {
                        XposedBridge.log("El nombre de la clase  ->" + clock.getClass().getSimpleName());
                        XposedBridge.log("before ->" + Arrays.toString(clock.getClass().getDeclaredFields()));

                        Field fieldName = clock.getClass().getDeclaredField("mCityName");
                        String nombre = (String) fieldName.get(clock);
                        XposedBridge.log("Nombre ciudad ->" + nombre);

                        Field field = clock.getClass().getDeclaredField("mTimeZone");
                        String timeZone = (String) field.get(clock);

                        long currentTime = System.currentTimeMillis();
                        int edtOffset = TimeZone.getTimeZone(timeZone).getOffset(currentTime);
                        int gmtOffset = TimeZone.getDefault().getOffset(currentTime);
                        int hourDifference = (gmtOffset - edtOffset) / (1000 * 60 * 60);
                        String diff = String.valueOf(hourDifference);

                        XposedBridge.log("Diferencia horas ->" + diff);
                        Calendar now = Calendar.getInstance();
                        now.add(Calendar.HOUR, Integer.parseInt(diff));
                        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                        String formattedDate = df.format(now.getTime());
                        XposedBridge.log("hour ->" + formattedDate);

                        XposedBridge.log("nombre-> "+nombre+"hour ->" + formattedDate);

                        editor.putString(nombre, formattedDate);
                        editor.apply();
                    }
                    for (int i = 0; i < param.args.length; i++) {
                        XposedBridge.log("before ->" + param.args[i].getClass());
                    }
                    XposedBridge.log(String.format("before ->%s", Arrays.toString(param.args[0].getClass().getDeclaredFields())));

                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Estamos dentro1 de el paquete");
                    XposedBridge.log("after ->" + param.args[0].getClass());
                }
            });
        } else if (lpparam.packageName.equals(SYSTEM_UI)) {


            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // this will be called before the clock was updated by the original method
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    TextView tv = (TextView) param.thisObject;
                    tv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Diff");

//                            Map<String,?> keys = prefs.getAll();
//                            XposedBridge.log("count ->" + keys.size());
//
//                            for(Map.Entry<String,?> entry : keys.entrySet()){
//                                sb.append(entry.getKey()).append(" -- ").append(entry.getValue().toString());
//                            }

                            String toastText = "Madrid - 10:00 \nLondon - 09:00 \nNew York - 04:00";
                            Toast.makeText(AndroidAppHelper.currentApplication().getApplicationContext(), toastText, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences("com.estudio.xposeddemo");
        prefs.makeWorldReadable();

    }
}