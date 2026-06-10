package com.duc.vcam;

import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import java.io.File;

public class XposedHook implements IXposedHookLoadPackage {

    private static final String FAKE_VIDEO_PATH = "/sdcard/Movies/fake.mp4";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log("[OmniVCam-Ant] Kích hoạt bẻ khóa Camera cho app: " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(Camera.class, "setPreviewTexture", SurfaceTexture.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = new File(FAKE_VIDEO_PATH);
                if (file.exists()) {
                    XposedBridge.log("[OmniVCam-Ant] Tìm thấy fake.mp4, tiến hành nạp đè...");
                }
            }
        });
    }
}
