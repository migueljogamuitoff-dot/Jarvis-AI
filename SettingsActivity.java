package com.seuprojeto.nexus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText editKey = findViewById(R.id.editApiKey);
        EditText editModel = findViewById(R.id.editModel);
        Button btnSave = findViewById(R.id.btnSaveSettings);

        SharedPreferences prefs = SecurePrefs.get(this);
        editKey.setText(prefs.getString("api_key", ""));
        String model = prefs.getString("model", "");
        editModel.setText(TextUtils.isEmpty(model) ? "claude-sonnet-5-5" : model);

        btnSave.setOnClickListener(v -> {
            prefs.edit()
                    .putString("api_key", editKey.getText().toString().trim())
                    .putString("model", editModel.getText().toString().trim())
                    .apply();
            Toast.makeText(this, "Configurações salvas.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
