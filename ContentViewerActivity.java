package com.seuprojeto.nexus;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ContentViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_viewer);

        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");

        TextView txtTitle = findViewById(R.id.txtContentTitle);
        txtTitle.setText(title != null ? title : "Conteúdo");

        WebView webView = findViewById(R.id.webViewContent);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        if (url != null) webView.loadUrl(url);

        Button btnBack = findViewById(R.id.btnVoltarNexus);
        btnBack.setOnClickListener(v -> finish());
    }
}
