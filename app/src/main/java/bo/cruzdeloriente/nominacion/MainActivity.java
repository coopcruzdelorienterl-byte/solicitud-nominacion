package bo.cruzdeloriente.nominacion;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

public class MainActivity extends Activity {
    private static final int SHARE_REQUEST_CODE = 4107;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(0, 75, 41));
        getWindow().setNavigationBarColor(Color.rgb(0, 75, 41));
        getWindow().getDecorView().setSystemUiVisibility(0);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(242, 247, 244));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidShareBridge(), "AndroidShare");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    public class AndroidShareBridge {
        @JavascriptInterface
        public void sharePng(String dataUrl, String filename) {
            try {
                byte[] bytes = decodeDataUrl(dataUrl);
                File dir = new File(getCacheDir(), "shared");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("No se pudo crear carpeta temporal");
                String safeName = safeFilename(filename);
                File file = new File(dir, safeName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(bytes);
                    out.flush();
                }
                Uri uri = FileProvider.getUriForFile(MainActivity.this,
                        getPackageName() + ".fileprovider", file);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("image/png");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.putExtra(Intent.EXTRA_SUBJECT, "Solicitud de nominación");
                send.putExtra(Intent.EXTRA_TEXT, "Comprobante " + safeName.replace(".png", ""));
                send.setClipData(ClipData.newUri(getContentResolver(), "Comprobante", uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivityForResult(Intent.createChooser(send, "Compartir comprobante"), SHARE_REQUEST_CODE));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "No se pudo compartir la imagen: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface
        public void savePng(String dataUrl, String filename) {
            try {
                byte[] bytes = decodeDataUrl(dataUrl);
                String safeName = safeFilename(filename);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, safeName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Cruz del Oriente");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("No se pudo crear el archivo en Imágenes");
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new Exception("No se pudo abrir el archivo");
                    out.write(bytes);
                    out.flush();
                }
                ContentValues done = new ContentValues();
                done.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, done, null, null);
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Comprobante guardado en Imágenes/Cruz del Oriente", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "No se pudo guardar la imagen: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHARE_REQUEST_CODE && webView != null) {
            // Al volver del selector de compartir, ofrecer iniciar una nueva nominación.
            // El usuario confirma con el botón NUEVA SOLICITUD; no se borran datos automáticamente.
            webView.post(() -> webView.evaluateJavascript(
                    "if (window.onAndroidShareReturned) window.onAndroidShareReturned();", null));
        }
    }

    private byte[] decodeDataUrl(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) throw new IllegalArgumentException("Imagen PNG no válida");
        String payload = dataUrl.substring(comma + 1);
        return Base64.getDecoder().decode(payload);
    }

    private String safeFilename(String filename) {
        String safe = filename == null ? "solicitud-nominacion.png" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!safe.toLowerCase().endsWith(".png")) safe += ".png";
        return safe;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
