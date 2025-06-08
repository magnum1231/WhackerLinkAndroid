package com.murrayh.whackerlinkandroid;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final String PREFS_NAME = "AppSettings";
    private static final int MIC_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);


        webView.addJavascriptInterface(new WebAppInterface(this, webView), "AndroidBridge");

        webView.loadUrl("file:///android_asset/index.html");

        requestMicrophonePermission();

        webView.postDelayed(() -> {
            sendSettingsToWebView();
            sendBatteryStatus();
        }, 500);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean hideActionBar = prefs.getBoolean("hideActionBar", false);
        boolean hideStatusBar = prefs.getBoolean("hideStatusBar", false);

        if (hideStatusBar) {
            hideSystemUI();
        }

        if (hideActionBar) {
            new Handler(Looper.getMainLooper()).postDelayed(this::hideActionBar, 10000);
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void hideActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean refresh = result.getData().getBooleanExtra("refresh", false);
                    if (refresh) {
                        reloadWebView(null);
                    }
                }
            });

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            if (event.getKeyCode() == prefs.getInt("powerToggle", -1)) {
                sendKeyPress("powerToggle");
                return true;
            }
            if (event.getKeyCode() == prefs.getInt("channelUp", -1)) {
                sendKeyPress("channelUp");
                return true;
            }
            if (event.getKeyCode() == prefs.getInt("channelDown", -1)) {
                sendKeyPress("channelDown");
                return true;
            }
            if (event.getKeyCode() == prefs.getInt("emerBtn", -1)) {
                sendKeyPress("emerBtn");
                return true;
            }
            if (event.getKeyCode() == prefs.getInt("PTT", -1)) {
                sendKeyPress("PTTdn");
                return true;
            }
        }else if (event.getAction() == KeyEvent.ACTION_UP) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            if (event.getKeyCode() == prefs.getInt("PTT", -1)) {
                sendKeyPress("PTTup");
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @JavascriptInterface
    public void sendKeyPress(String button) {
        webView.post(() -> webView.evaluateJavascript("onHardwareButtonPress('" + button + "')", null));
    }

    @JavascriptInterface
    public void sendBatteryStatus(int level) {
        webView.post(() -> webView.evaluateJavascript("onBatteryStatus(" + level + ")", null));
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        settingsLauncher.launch(intent);
    }

    public void reloadWebView(View view) {
        if (webView != null) {
            webView.loadUrl("file:///android_asset/index.html");
            webView.postDelayed(() -> {
                sendSettingsToWebView();
                sendBatteryStatus();
            }, 500);
        }
    }

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone access granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone access denied! Please enable it in settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            runOnUiThread(() -> {
                // Grant all requested permissions
                request.grant(request.getResources());
            });
        }
    }

    private void sendBatteryStatus() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryPercentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        int level = batteryPercentage < 20 ? 0 :
                batteryPercentage < 40 ? 1 :
                        batteryPercentage < 60 ? 2 :
                                batteryPercentage < 80 ? 3 : 4;

        webView.post(() -> webView.evaluateJavascript("onBatteryStatus(" + level + ")", null));
    }

    private void sendSettingsToWebView() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String selectedYAML = prefs.getString("selectedYAML", "codeplug.basic.yml");

        String yamlPath;
        if (selectedYAML.equals("codeplug.basic.yml")) {
            yamlPath = "file:///android_asset/codeplug.basic.yml";
        } else {
            yamlPath = "file://" + selectedYAML;
        }

        String yamlContent = loadYAMLContent(selectedYAML);
        boolean onScreenControls = prefs.getBoolean("onScreenControls", false);
        String customString = prefs.getString("customString", "DefaultString");

        yamlContent = yamlContent.replace("\n", "\\n").replace("'", "\\'");

        final String jsCode = "onReceiveSettings(" +
                "'" + yamlPath + "', " +
                onScreenControls + ", " +
                "'" + customString + "', " +
                "'" + yamlContent + "'" +
                ");";

        webView.post(() -> webView.evaluateJavascript(jsCode, null));
    }

    private String loadYAMLContent(String filePath) {
        try {
            if (filePath.equals("codeplug.basic.yml")) {
                InputStream is = getAssets().open("codeplug.basic.yml");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            } else {
                File file = new File(filePath);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            }
        } catch (IOException e) {
            return "Error loading YAML file.";
        }
    }

    public static class WebAppInterface {
        private final Context context;
        private final WebView webView;

        WebAppInterface(Context context, WebView webView) {
            this.context = context;
            this.webView = webView;
        }
    }
}
