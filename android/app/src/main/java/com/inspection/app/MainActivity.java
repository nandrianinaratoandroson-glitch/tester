package com.votre.app; // ← remplacer par votre package

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.media.MediaScannerConnection;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);      // localStorage (sauvegarde automatique)
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());

        // ← LIGNE IMPORTANTE : connecte AndroidBridge au JavaScript
        webView.addJavascriptInterface(new AndroidBridge(this, webView), "AndroidBridge");

        // Charger votre fichier HTML embarqué dans les assets
        webView.loadUrl("file:///android_asset/index.html");

        // OU si hébergé localement :
        // webView.loadUrl("http://localhost/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    // ================================================================
    //  AndroidBridge — Pont entre JavaScript et Android natif
    //  Permet à votre HTML de sauvegarder des fichiers sur le téléphone
    // ================================================================
    public class AndroidBridge {

        private Activity activity;
        private WebView webView;

        AndroidBridge(Activity a, WebView wv) {
            this.activity = a;
            this.webView = wv;
        }

        /**
         * Appelé depuis JavaScript :
         *   AndroidBridge.saveBase64File(base64String, "Rapport.xlsx")
         *
         * Sauvegarde le fichier dans le dossier Téléchargements du téléphone.
         */
        @JavascriptInterface
        public void saveBase64File(final String base64Data, final String fileName) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Décoder le base64 en bytes
                        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

                        // Dossier Téléchargements public
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadsDir.exists()) downloadsDir.mkdirs();

                        File outputFile = new File(downloadsDir, fileName);

                        // Écrire le fichier
                        FileOutputStream fos = new FileOutputStream(outputFile);
                        fos.write(bytes);
                        fos.flush();
                        fos.close();

                        // Notifier le système pour que le fichier apparaisse dans
                        // l'application Fichiers et le gestionnaire de téléchargements
                        MediaScannerConnection.scanFile(
                                activity,
                                new String[]{outputFile.getAbsolutePath()},
                                new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                                null
                        );

                        // Informer le JavaScript que c'est réussi
                        webView.evaluateJavascript(
                                "onFileSaved(true, '" + fileName + "')", null);

                    } catch (Exception e) {
                        // Informer le JavaScript de l'erreur
                        webView.evaluateJavascript(
                                "onFileSaved(false, '')", null);
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
