package its.madruga.wpp.xposed.plugins.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.ClassesReference;
import its.madruga.wpp.listeners.RestartListener;
import its.madruga.wpp.xposed.plugins.functions.XAntiRevoke;
import its.madruga.wpp.xposed.plugins.functions.XDndMode;
import its.madruga.wpp.xposed.plugins.functions.XMediaQuality;
import its.madruga.wpp.xposed.plugins.functions.XNewChat;
import its.madruga.wpp.xposed.plugins.functions.XOthers;
import its.madruga.wpp.xposed.plugins.functions.XShareLimit;
import its.madruga.wpp.xposed.plugins.functions.XStatusDownload;
import its.madruga.wpp.xposed.plugins.functions.XViewOnce;
import its.madruga.wpp.xposed.plugins.personalization.XBioAndName;
import its.madruga.wpp.xposed.plugins.personalization.XBubbleColors;
import its.madruga.wpp.xposed.plugins.personalization.XChangeColors;
import its.madruga.wpp.xposed.plugins.personalization.XChatsFilter;
import its.madruga.wpp.xposed.plugins.personalization.XSecondsToTime;
import its.madruga.wpp.xposed.plugins.privacy.XFreezeLastSeen;
import its.madruga.wpp.xposed.plugins.privacy.XGhostMode;
import its.madruga.wpp.xposed.plugins.privacy.XHideReceipt;
import its.madruga.wpp.xposed.plugins.privacy.XHideTag;
import its.madruga.wpp.xposed.plugins.privacy.XHideView;

public class XMain {

    public static Application mApp;
    public static ArrayList<String> list = new ArrayList<>();

    public static void Initialize(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref) {

        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mApp = (Application) param.args[0];
                PackageManager packageManager = mApp.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo("com.whatsapp", 0);
                XposedBridge.log(packageInfo.versionName);
                if (packageInfo.versionName.equals(BuildConfig.VERSION_NAME)) {
                    XposedBridge.log("Loading whatsapp - correct version");
                    plugins(loader, pref);
                }
            }
        });

        XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity", loader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                XposedBridge.log("List size: " + list.size());
                if (!list.isEmpty()) {
                    new AlertDialog.Builder((Activity) param.thisObject)
                            .setTitle("Error detected")
                            .setMessage("The following options aren't working:\n\n" + String.join("\n", list.toArray(new String[0])))
                            .show();
                }
            }
        });

        RestartListener.start(XposedHelpers.findClass(ClassesReference.AutoReboot.autoreboot, loader));
    }

    private static void plugins(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref) {
        ArrayList<String> loadedClasses = new ArrayList<>();
        var classes = new Class<?>[]{
                XOthers.class,
                XDndMode.class,
                XHideTag.class,
                XHideView.class,
                XViewOnce.class,
                XGhostMode.class,
                XAntiRevoke.class,
                XBioAndName.class,
                XShareLimit.class,
                XChatsFilter.class,
                XHideReceipt.class,
                XChangeColors.class,
                XMediaQuality.class,
                XBubbleColors.class,
                XSecondsToTime.class,
                XStatusDownload.class,
                XStatusDownload.class,
                XFreezeLastSeen.class,
                XNewChat.class
        };

        for (var classe : classes) {
            try {
                var constructor = classe.getConstructor(ClassLoader.class, XSharedPreferences.class);
                var plugin = constructor.newInstance(loader, pref);
                var method = classe.getMethod("doHook");
                method.invoke(plugin);
                loadedClasses.add("-> "  +classe.getName());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                     InstantiationException e) {
                XposedBridge.log(e);
                if (e instanceof InvocationTargetException) {
                    list.add(classe.getSimpleName());
                }
            }
        }

        XposedBridge.log("Loaded classes:\n\n" + String.join("\n", loadedClasses.toArray(new String[0])));
    }
}
