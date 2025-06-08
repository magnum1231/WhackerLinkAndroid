package com.murrayh.whackerlinkandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class SettingsActivity extends Activity {

    private static final String PREFS_NAME = "AppSettings";
    private static final int FILE_PICKER_REQUEST_CODE = 1;

    private TextView infoText;
    private Button powerButton, upButton, downButton, emerButton, pttButton, saveExitButton, addYamlButton;
    private Switch onScreenControlsSwitch, hideActionBarSwitch, hideStatusBarSwitch;
    private ListView yamlListView;
    private EditText customStringInput;
    private ArrayAdapter<String> yamlAdapter;
    private ArrayList<String> yamlFiles;
    private String waitingForKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        infoText = findViewById(R.id.infoText);
        powerButton = findViewById(R.id.btnPower);
        upButton = findViewById(R.id.btnUp);
        downButton = findViewById(R.id.btnDown);
        emerButton = findViewById(R.id.btnEmer);
        onScreenControlsSwitch = findViewById(R.id.switchOnScreenControls);
        yamlListView = findViewById(R.id.yamlList);
        customStringInput = findViewById(R.id.customStringInput);
        saveExitButton = findViewById(R.id.btnSaveExit);
        addYamlButton = findViewById(R.id.btnAddYAML);
        pttButton = findViewById(R.id.btnPTT);

        hideActionBarSwitch = findViewById(R.id.switchHideActionBar);
        hideStatusBarSwitch = findViewById(R.id.switchHideStatusBar);

        loadButtonBindings();
        loadYAMLFiles();
        loadOnScreenControls();
        loadCustomString();

        powerButton.setOnClickListener(v -> waitForKeyPress("powerToggle"));
        upButton.setOnClickListener(v -> waitForKeyPress("channelUp"));
        downButton.setOnClickListener(v -> waitForKeyPress("channelDown"));
        emerButton.setOnClickListener(v -> waitForKeyPress("emerBtn"));
        pttButton.setOnClickListener(v -> waitForKeyPress("PTT"));

        yamlListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFile = yamlFiles.get(position);
            saveSelectedYAML(selectedFile);
            Toast.makeText(this, "Selected YAML: " + selectedFile, Toast.LENGTH_SHORT).show();
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        hideActionBarSwitch.setChecked(prefs.getBoolean("hideActionBar", false));
        hideStatusBarSwitch.setChecked(prefs.getBoolean("hideStatusBar", false));
        onScreenControlsSwitch.setChecked(prefs.getBoolean("onScreenControls", false));

        hideActionBarSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("hideActionBar", isChecked).apply();
        });

        hideStatusBarSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("hideStatusBar", isChecked).apply();
        });

        onScreenControlsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("onScreenControls", isChecked).apply();
        });

        saveExitButton.setOnClickListener(v -> {
            applySettings();
            finish();
        });

        addYamlButton.setOnClickListener(v -> openFilePicker());
    }

    private void waitForKeyPress(String buttonName) {
        waitingForKey = buttonName;
        infoText.setText("Press a button for " + buttonName);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (waitingForKey != null) {
            saveBinding(waitingForKey, keyCode);
            waitingForKey = null;
            infoText.setText("Key saved!");
            loadButtonBindings();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void saveBinding(String buttonName, int keyCode) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(buttonName, keyCode).apply();
    }

    private void loadButtonBindings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        powerButton.setText("Power: " + getKeyLabel(prefs.getInt("powerToggle", -1)));
        upButton.setText("Channel Up: " + getKeyLabel(prefs.getInt("channelUp", -1)));
        downButton.setText("Channel Down: " + getKeyLabel(prefs.getInt("channelDown", -1)));
        emerButton.setText("Emergency: " + getKeyLabel(prefs.getInt("emerBtn", -1)));
        pttButton.setText("PTT: " + getKeyLabel(prefs.getInt("PTT", -1)));
    }

    private void loadOnScreenControls() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        onScreenControlsSwitch.setChecked(prefs.getBoolean("onScreenControls", false));
    }

    private String getKeyLabel(int keyCode) {
        return (keyCode == -1) ? "Not Set" : KeyEvent.keyCodeToString(keyCode);
    }

    private void loadYAMLFiles() {
        yamlFiles = new ArrayList<>();
        yamlFiles.add("codeplug.basic.yml"); // Default YAML file

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedYAML = prefs.getString("yamlFiles", "");

        if (!savedYAML.isEmpty()) {
            String[] files = savedYAML.split(";");
            for (String file : files) {
                if (!yamlFiles.contains(file)) {
                    yamlFiles.add(file);
                }
            }
        }

        yamlAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, yamlFiles);
        yamlListView.setAdapter(yamlAdapter);
    }

    private void saveSelectedYAML(String filePath) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("selectedYAML", filePath).apply();
    }

    private void loadCustomString() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        customStringInput.setText(prefs.getString("customString", "0000"));
    }

    private void applySettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("customString", customStringInput.getText().toString()).apply();

        Intent intent = new Intent();
        intent.putExtra("refresh", true);
        setResult(RESULT_OK, intent);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType("text/yaml");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select YAML File"), FILE_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String filePath = uri.getPath(); // Get file path
                if (filePath != null) {
                    yamlFiles.add(filePath);
                    yamlAdapter.notifyDataSetChanged();
                    saveSelectedYAML(filePath);
                    Toast.makeText(this, "Added YAML: " + filePath, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
