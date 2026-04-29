package com.acooldog.toolbox.root;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.elvishew.xlog.XLog;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class RootTestAuditLogger {
    private static final String KEY_ALIAS = "root_test_audit_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final String PREFS_NAME = "root_test_audit_store";
    private static final String KEY_EVENTS = "events";
    private static final String AUDIT_FILE_NAME = "root_test_audit.log.enc";
    private static final int MAX_EVENTS = 40;

    private final Context appContext;
    private final SharedPreferences preferences;
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public RootTestAuditLogger(@NonNull Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void append(@NonNull String event) {
        String entry = timestampFormat.format(new Date()) + " " + event;
        try {
            String encryptedEntry = encryptToBase64(entry);
            List<String> entries = getEncryptedEntries();
            entries.add(0, encryptedEntry);
            while (entries.size() > MAX_EVENTS) {
                entries.remove(entries.size() - 1);
            }
            JSONArray array = new JSONArray();
            for (String item : entries) {
                array.put(item);
            }
            preferences.edit().putString(KEY_EVENTS, array.toString()).apply();
            appendEncryptedLine(encryptedEntry);
            XLog.i("RootTestAudit encrypted hash=" + sha256(entry));
        } catch (Exception exception) {
            XLog.e("RootTestAudit encryption failed: " + exception.getClass().getSimpleName());
        }
    }

    @NonNull
    public List<String> getRecentEntries() {
        List<String> decryptedEntries = new ArrayList<>();
        for (String encryptedEntry : getEncryptedEntries()) {
            try {
                String entry = decryptFromBase64(encryptedEntry);
                if (!entry.trim().isEmpty()) {
                    decryptedEntries.add(entry);
                }
            } catch (Exception ignored) {
                // Ignore legacy plaintext or corrupted encrypted entries.
            }
        }
        return decryptedEntries;
    }

    @NonNull
    public File getEncryptedAuditFile() {
        return new File(appContext.getFilesDir(), AUDIT_FILE_NAME);
    }

    @NonNull
    private List<String> getEncryptedEntries() {
        List<String> entries = new ArrayList<>();
        String raw = preferences.getString(KEY_EVENTS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                String item = array.optString(index, "");
                if (!item.trim().isEmpty()) {
                    entries.add(item);
                }
            }
        } catch (Exception ignored) {
            // Ignore corrupted local audit cache.
        }
        return entries;
    }

    private void appendEncryptedLine(@NonNull String encryptedEntry) {
        File auditFile = getEncryptedAuditFile();
        try (FileOutputStream outputStream = new FileOutputStream(auditFile, true)) {
            outputStream.write(encryptedEntry.getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
        } catch (Exception exception) {
            XLog.e("RootTestAudit file write failed: " + exception.getClass().getSimpleName());
        }
    }

    @NonNull
    private String encryptToBase64(@NonNull String value) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    @NonNull
    private String decryptFromBase64(@NonNull String value) throws Exception {
        byte[] combined = Base64.decode(value, Base64.NO_WRAP);
        if (combined.length <= 12) {
            throw new IllegalArgumentException("encrypted audit entry is too short");
        }
        byte[] iv = new byte[12];
        byte[] cipherText = new byte[combined.length - iv.length];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    @NonNull
    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    @NonNull
    private String sha256(@NonNull String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            builder.append(String.format(Locale.US, "%02x", item));
        }
        return builder.toString();
    }
}
