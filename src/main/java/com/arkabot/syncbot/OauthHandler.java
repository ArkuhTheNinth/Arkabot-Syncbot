package com.arkabot.syncbot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class OauthHandler {
    private static final String DROPBOX_APP_KEY = "qwnd8vi11pc7iiv"; // Replace with your Dropbox app key
    private static final String DROPBOX_REDIRECT_URI = "https://arkuhtheninth.github.io/dropbox-oauth-redirect/"; // Replace with your redirect URI
    private Context context;

    public OauthHandler(Context context) {
        this.context = context;
    }

    public void startDropboxOAuth() {
        String authorizeUrl = "https://www.dropbox.com/oauth2/authorize"
                + "?client_id=" + Uri.encode(DROPBOX_APP_KEY)
                + "&response_type=token"
                + "&redirect_uri=" + Uri.encode(DROPBOX_REDIRECT_URI)
                + "&force_reapprove=true"; // Force reapproval even if user is logged in
        FileLogger.log(context, "Starting OAuth with URL: " + authorizeUrl);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback to any browser if Chrome is not installed
            intent.setPackage(null);
            context.startActivity(intent);
        }
    }

    public void handleRedirectUri(String url) {
        FileLogger.log(context, "Handling redirect URI: " + url);
        if (url != null && url.startsWith(DROPBOX_REDIRECT_URI)) {
            Uri uri = Uri.parse(url);
            String fragment = uri.getFragment();
            FileLogger.log(context, "Parsed fragment: " + fragment);
            if (fragment != null) {
                String[] params = fragment.split("&");
                for (String param : params) {
                    if (param.startsWith("access_token=")) {
                        String accessToken = param.split("=")[1];
                        FileLogger.log(context, "Access token found: " + accessToken);
                        saveAccessToken(accessToken);
                        ((MainActivity) context).initializeViews();
                        return;
                    }
                }
            }
            FileLogger.log(context, "Error: No access token found in the URL fragment.");
        }
    }

    private void saveAccessToken(String accessToken) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("dropbox_access_token", accessToken)
                .apply();
        FileLogger.log(context, "Access token saved successfully.");
    }
}
