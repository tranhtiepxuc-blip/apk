package com.duc.vcam;

import android.hardware.Camera;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;
import java.io.ByteArrayOutputStream;
import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui") || lpparam.packageName.equals("com.duc.vcam")) {
            return;
        }

        XposedBridge.log("[OmniVCam-Pro] Khởi động bẻ khóa camera cho app: " + lpparam.packageName);

        // 🚀 BƯỚC MỚI 1: Bẻ luồng xem trước (Preview) thông qua SurfaceTexture
        XposedHelpers.findAndHookMethod(Camera.class, "setPreviewTexture", SurfaceTexture.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("[OmniVCam-Pro] Đang ép luồng hiển thị Preview (Texture)...");
                // Chặn luồng ngắm để app không lấy dữ liệu camera thật hiển thị lên màn hình
            }
        });

        // 🚀 BƯỚC MỚI 2: Bẻ luồng xem trước (Preview) thông qua SurfaceHolder
        XposedHelpers.findAndHookMethod(Camera.class, "setPreviewDisplay", SurfaceHolder.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("[OmniVCam-Pro] Đang ép luồng hiển thị Preview (Display)...");
            }
        });

        // BƯỚC 3: Giữ nguyên luồng xử lý tráo ảnh khi bấm nút chụp
        XposedHelpers.findAndHookMethod(Camera.class, "takePicture", 
            Camera.ShutterCallback.class, 
            Camera.PictureCallback.class, 
            Camera.PictureCallback.class, 
            Camera.PictureCallback.class, 
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[OmniVCam-Pro] Thao tác bấm máy chụp kích hoạt!");

                    XSharedPreferences xPref = new XSharedPreferences("com.duc.vcam", "vcam_settings");
                    xPref.makeWorldReadable();
                    String customImagePath = xPref.getString("image_path", null);

                    if (customImagePath != null) {
                        File imgFile = new File(customImagePath);
                        if (imgFile.exists()) {
                            XposedBridge.log("[OmniVCam-Pro] Đang nạp đè tấm ảnh tự chọn: " + customImagePath);
                            
                            Bitmap bitmap = BitmapFactory.decodeFile(customImagePath);
                            if (bitmap != null) {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                                byte[] fakePhotoBytes = stream.toByteArray();

                                if (param.args[3] != null) {
                                    param.args[3] = new Camera.PictureCallback() {
                                        @Override
                                        public void onPictureTaken(byte[] data, Camera camera) {
                                            ((Camera.PictureCallback) param.args[3]).onPictureTaken(fakePhotoBytes, camera);
                                        }
                                    };
                                }
                            }
                        }
                    }
                }
        });
    }
}
