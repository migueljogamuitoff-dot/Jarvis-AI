package com.seuprojeto.nexus;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DevicesActivity extends AppCompatActivity {

    private ListView listView;
    private List<DeviceStore.HttpDevice> devices;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        listView = findViewById(R.id.listDevices);
        Button btnAdd = findViewById(R.id.btnAddDevice);

        refreshList();
        btnAdd.setOnClickListener(v -> showAddDialog());
    }

    private void refreshList() {
        devices = DeviceStore.load(this);
        List<String> names = new ArrayList<>();
        for (DeviceStore.HttpDevice d : devices) names.add(d.name);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            devices.remove(position);
            DeviceStore.save(this, devices);
            refreshList();
            return true;
        });
    }

    private void showAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText editName = new EditText(this);
        editName.setHint("Nome (ex: tomada da sala)");
        EditText editOn = new EditText(this);
        editOn.setHint("URL para ligar (http://...)");
        EditText editOff = new EditText(this);
        editOff.setHint("URL para desligar (http://...)");

        layout.addView(editName);
        layout.addView(editOn);
        layout.addView(editOff);

        new AlertDialog.Builder(this)
                .setTitle("Novo dispositivo")
                .setView(layout)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    devices.add(new DeviceStore.HttpDevice(
                            name,
                            editOn.getText().toString().trim(),
                            editOff.getText().toString().trim()));
                    DeviceStore.save(this, devices);
                    refreshList();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
