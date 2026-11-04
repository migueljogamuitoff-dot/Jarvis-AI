package com.seuprojeto.nexus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQ_RECORD_AUDIO = 101;
    private static final int REQ_SPEECH = 102;
    private static final int REQ_OVERLAY = 103;

    private TextView txtClock, txtBattery, txtRam, txtUptime, txtMicState;
    private LinearLayout logContainer;
    private ScrollView scrollLog;
    private EditText editMessage;
    private OrbView orbView;

    private TextToSpeech tts;
    private final SfxPlayer sfx = new SfxPlayer();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long startTime = SystemClock.elapsedRealtime();
    private final List<String[]> history = new ArrayList<>();
    private volatile boolean requestInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtClock = findViewById(R.id.txtClock);
        txtBattery = findViewById(R.id.txtStatBattery);
        txtRam = findViewById(R.id.txtStatRam);
        txtUptime = findViewById(R.id.txtStatUptime);
        txtMicState = findViewById(R.id.txtMicState);
        logContainer = findViewById(R.id.logContainer);
        scrollLog = findViewById(R.id.scrollLog);
        editMessage = findViewById(R.id.editMessage);
        orbView = findViewById(R.id.orbView);

        tts = new TextToSpeech(this, this);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { }
            @Override public void onDone(String utteranceId) {
                runOnUiThread(() -> orbView.setState(OrbView.State.IDLE));
            }
            @Override public void onError(String utteranceId) {
                runOnUiThread(() -> orbView.setState(OrbView.State.IDLE));
            }
        });

        findViewById(R.id.btnSend).setOnClickListener(v -> handleSend());
        findViewById(R.id.btnMic).setOnClickListener(v -> startListening());
        findViewById(R.id.btnInterrupt).setOnClickListener(v -> interruptNexus());
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnDevices).setOnClickListener(v ->
                startActivity(new Intent(this, DevicesActivity.class)));
        findViewById(R.id.btnVision).setOnClickListener(v ->
                startActivity(new Intent(this, VisionActivity.class)));
        findViewById(R.id.btnBubble).setOnClickListener(v -> toggleBubble());
        findViewById(R.id.btnCastTv).setOnClickListener(v -> openCastSettings());

        appendLog("SYS", "Núcleo Nexus iniciado.");
        if (TextUtils.isEmpty(getApiKey())) {
            appendLog("SYS", "Nenhuma chave de API configurada. Toque em ⚙ para adicionar sua chave da Anthropic.");
        } else {
            appendLog("NEXUS", "Olá! Em que posso ajudar?");
        }

        startClock();
        startStats();
    }

    private String getApiKey() {
        return SecurePrefs.get(this).getString("api_key", "");
    }

    private String getModel() {
        String m = SecurePrefs.get(this).getString("model", "");
        return TextUtils.isEmpty(m) ? "claude-sonnet-5-5" : m;
    }

    private void startClock() {
        handler.post(new Runnable() {
            @Override public void run() {
                SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                txtClock.setText(fmt.format(new java.util.Date()));
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void startStats() {
        handler.post(new Runnable() {
            @Override public void run() {
                int bat = SystemStatsHelper.batteryPercent(MainActivity.this);
                txtBattery.setText("BAT " + (bat >= 0 ? bat + "%" : "N/A"));
                txtRam.setText("RAM " + SystemStatsHelper.ramUsage(MainActivity.this));
                txtUptime.setText("UP " + SystemStatsHelper.uptime(startTime));
                handler.postDelayed(this, 3000);
            }
        });
    }

    private void appendLog(String tag, String message) {
        TextView line = new TextView(this);
        line.setText(tag + ": " + message);
        line.setTextSize(13f);
        line.setPadding(0, 4, 0, 4);
        int color;
        switch (tag) {
            case "VOCÊ": color = getColor(R.color.nexus_text); break;
            case "NEXUS": color = getColor(R.color.nexus_violet); break;
            default: color = getColor(R.color.nexus_cyan);
        }
        line.setTextColor(color);
        logContainer.addView(line);
        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    private void handleSend() {
        String text = editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) || requestInFlight) return;
        editMessage.setText("");
        appendLog("VOCÊ", text);
        sfx.playConfirm();

        String apiKey = getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            appendLog("SYS", "Configure sua chave de API em ⚙ Configurações para conversar com a IA.");
            return;
        }

        requestInFlight = true;
        orbView.setState(OrbView.State.THINKING);
        appendLog("SYS", "processando...");

        AiClient.sendMessage(apiKey, getModel(), history, text, new AiClient.Callback() {
            @Override
            public void onSuccess(String reply) {
                requestInFlight = false;
                history.add(new String[]{"user", text});
                history.add(new String[]{"assistant", reply});
                runOnUiThread(() -> {
                    appendLog("NEXUS", reply);
                    speak(reply);
                });
            }

            @Override
            public void onToolCall(String toolName, JSONObject input, AiClient.ToolResultCallback resultCallback) {
                runOnUiThread(() -> appendLog("SYS", "executando: " + toolName + "..."));
                executeTool(toolName, input, resultCallback);
            }

            @Override
            public void onError(String message) {
                requestInFlight = false;
                runOnUiThread(() -> {
                    orbView.setState(OrbView.State.IDLE);
                    appendLog("SYS", message);
                    sfx.playError();
                });
            }
        });
    }

    private void executeTool(String toolName, JSONObject input, AiClient.ToolResultCallback resultCallback) {
        switch (toolName) {
            case "control_device": {
                String deviceName = input.optString("device_name", "");
                String action = input.optString("action", "");
                List<DeviceStore.HttpDevice> devices = DeviceStore.load(this);
                DeviceStore.HttpDevice found = null;
                String needle = deviceName.toLowerCase(Locale.getDefault());
                for (DeviceStore.HttpDevice d : devices) {
                    String hay = d.name.toLowerCase(Locale.getDefault());
                    if (hay.contains(needle) || needle.contains(hay)) {
                        found = d;
                        break;
                    }
                }
                if (found == null) {
                    resultCallback.onResult("dispositivo \"" + deviceName + "\" não encontrado nos cadastrados. Peça para o usuário cadastrar em Dispositivos.");
                    return;
                }
                String targetUrl = "ligar".equals(action) ? found.onUrl : found.offUrl;
                if (TextUtils.isEmpty(targetUrl)) {
                    resultCallback.onResult("dispositivo encontrado, mas sem URL cadastrada para essa ação.");
                    return;
                }
                DeviceStore.sendCommand(targetUrl,
                        () -> resultCallback.onResult("comando executado com sucesso."),
                        () -> resultCallback.onResult("falha ao contatar o dispositivo."));
                break;
            }
            case "control_music": {
                String action = input.optString("action", "");
                boolean ok = MusicController.dispatch(this, action);
                resultCallback.onResult(ok ? "comando de mídia enviado." : "ação de mídia desconhecida.");
                break;
            }
            case "open_content": {
                String url = input.optString("url", "");
                String title = input.optString("title", "Conteúdo");
                runOnUiThread(() -> {
                    Intent i = new Intent(MainActivity.this, ContentViewerActivity.class);
                    i.putExtra("url", url);
                    i.putExtra("title", title);
                    startActivity(i);
                });
                resultCallback.onResult("conteúdo aberto na tela.");
                break;
            }
            default:
                resultCallback.onResult("ferramenta desconhecida.");
        }
    }

    private void interruptNexus() {
        if (tts != null) tts.stop();
        requestInFlight = false;
        orbView.setState(OrbView.State.IDLE);
        appendLog("SYS", "Interrompido.");
    }

    private void speak(String text) {
        orbView.setState(OrbView.State.SPEAKING);
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus_reply");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            tts.setLanguage(new Locale("pt", "BR"));
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        txtMicState.setText("MICROFONE: OUVINDO");
        orbView.setState(OrbView.State.LISTENING);
        sfx.playActivate();
        try {
            startActivityForResult(intent, REQ_SPEECH);
        } catch (Exception e) {
            appendLog("SYS", "Reconhecimento de voz indisponível neste aparelho.");
            orbView.setState(OrbView.State.IDLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        txtMicState.setText("MICROFONE: OFF");
        if (requestCode == REQ_SPEECH) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    editMessage.setText(results.get(0));
                    handleSend();
                    return;
                }
            }
            orbView.setState(OrbView.State.IDLE);
        }
    }

    private void openCastSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_CAST_SETTINGS));
        } catch (Exception e) {
            appendLog("SYS", "Não encontrei o painel de transmissão deste aparelho. Procure \"Transmitir tela\" ou \"Smart View\" nas configurações rápidas.");
        }
    }

    private void toggleBubble() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
            return;
        }
        Intent service = new Intent(this, NexusBubbleService.class);
        startForegroundService(service);
        appendLog("SYS", "Bolinha flutuante ativada.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
