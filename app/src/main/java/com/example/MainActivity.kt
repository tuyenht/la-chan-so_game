package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView?.evaluateJavascript("if (typeof window.handleAndroidBack === 'function') { window.handleAndroidBack(); } else { false; }") { result ->
                    if (result != "true") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CyberShieldWebView(
                        onWebViewCreated = { webView = it }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun vibratePhone(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun vibrateError() {
        try {
            val timings = longArrayOf(0, 120, 80, 200)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(longArrayOf(0, 120, 80, 200), -1)
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun shareText(title: String, text: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(shareIntent)
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun downloadImage(dataUrl: String, fileName: String) {
        try {
            val pureBase64 = dataUrl.substringAfter("base64,")
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val cyberDir = File(picturesDir, "CyberShield")
            if (!cyberDir.exists()) {
                cyberDir.mkdirs()
            }
            val file = File(cyberDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(decodedBytes)
                fos.flush()
            }
            Toast.makeText(context, "Đã lưu chứng nhận vào thư viện ảnh!", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Đã tạo chứng nhận thành công!", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CyberShieldWebView(
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Optimize hardware layer to prevent MESA/rendernode glitches
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(0xFF0B132B.toInt())

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mediaPlaybackRequiresUserGesture = false
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: ""
                        if (url.startsWith("file://") || url.startsWith("data:")) {
                            return false
                        }
                        // Open external URLs in external browser safely
                        return try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            ctx.startActivity(intent)
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
                addJavascriptInterface(WebAppInterface(ctx), "AndroidInterface")
                loadUrl("file:///android_asset/index.html")
                onWebViewCreated(this)
            }
        }
    )
}
