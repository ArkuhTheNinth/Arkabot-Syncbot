package com.arkabot.syncbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TokenManager {
    private static final String PREFS_NAME = "token_prefs";
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String EXPIRY_TIME_KEY = "expiry_time";
    private static final String LAST_REFRESH_TIME_KEY = "last_refresh_time";
    private static final String ENCRYPTION_KEY = "MySecretKey12345"; // Replace with your actual encryption key
    private static final long REFRESH_INTERVAL = TimeUnit.MINUTES.toMillis(30); // 30 minutes

    private SharedPreferences sharedPreferences;
    private Context context;

    public TokenManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, long expiryTime) {
        try {
            String encryptedToken = encrypt(accessToken);
            sharedPreferences.edit()
                    .putString(ACCESS_TOKEN_KEY, encryptedToken)
                    .putLong(EXPIRY_TIME_KEY, expiryTime)
                    .apply();
            FileLogger.log(context, "Tokens saved successfully.");
        } catch (Exception e) {
            FileLogger.log(context, "Error saving tokens: " + e.getMessage());
        }
    }

    public String getAccessToken() {
        try {
            String encryptedToken = sharedPreferences.getString(ACCESS_TOKEN_KEY, null);
            if (encryptedToken != null) {
                return decrypt(encryptedToken);
            } else {
                return null;
            }
        } catch (Exception e) {
            FileLogger.log(context, "Error retrieving access token: " + e.getMessage());
            return null;
        }
    }

    public long getExpiryTime() {
        return sharedPreferences.getLong(EXPIRY_TIME_KEY, 0);
    }

    public long getLastRefreshTime() {
        return sharedPreferences.getLong(LAST_REFRESH_TIME_KEY, 0);
    }

    public void updateLastRefreshTime() {
        sharedPreferences.edit()
                .putLong(LAST_REFRESH_TIME_KEY, System.currentTimeMillis())
                .apply();
    }

    public boolean shouldRefreshToken() {
        long currentTime = System.currentTimeMillis();
        long lastRefreshTime = getLastRefreshTime();
        return currentTime - lastRefreshTime >= REFRESH_INTERVAL;
    }

    private String encrypt(String data) throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKey secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    private String decrypt(String encryptedData) throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKey secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT));
        return new String(decryptedBytes, "UTF-8");
    }
}
