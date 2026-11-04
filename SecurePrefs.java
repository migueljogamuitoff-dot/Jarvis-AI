package com.seuprojeto.nexus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePrefs {
    private static SharedPreferences instance;

    public static SharedPreferences get(Context context) {
        if (instance == null) {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                instance = EncryptedSharedPreferences.create(
                        context,
                        "nexus_secure_prefs",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                instance = context.getSharedPreferences("nexus_fallback_prefs", Context.MODE_PRIVATE);
            }
        }
        return instance;
    }
}
