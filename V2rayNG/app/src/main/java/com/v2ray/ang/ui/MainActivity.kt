package com.v2ray.ang.ui

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.R
import com.v2ray.ang.helper.FandoghApiService
import com.v2ray.ang.helper.AesDecryptor
import com.v2ray.ang.helper.PingHelper
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.core.CoreServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(WebAppInterface(), "FandoghApp")
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onConnectClick() {
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "🔍 در حال جستجوی بهترین سرور...", Toast.LENGTH_SHORT).show()
                try {
                    val configs = FandoghApiService.fetchConfigs()
                    if (configs.isNullOrEmpty()) {
                        Toast.makeText(this@MainActivity, "❌ سرور در دسترس نیست", Toast.LENGTH_LONG).show()
                        webView.evaluateJavascript("onScanError('سرور در دسترس نیست')", null)
                        return@launch
                    }

                    val decrypted = configs.map { cfg ->
                        val link = AesDecryptor.decrypt(cfg.encryptedPayload, "mi-9-lite-secure-hardware-id-12345")
                        cfg.copy(encryptedPayload = link)
                    }

                    val best = withContext(Dispatchers.IO) {
                        decrypted.minByOrNull { PingHelper.tcpPing(it.server, it.port) }
                    }

                    if (best == null) {
                        Toast.makeText(this@MainActivity, "⚠️ هیچ سروری پاسخگو نیست", Toast.LENGTH_LONG).show()
                        webView.evaluateJavascript("onScanError('هیچ سروری پاسخگو نیست')", null)
                        return@launch
                    }

                    val success = importVlessConfig(best.encryptedPayload)
                    if (success) {
                        CoreServiceManager.startVService(this@MainActivity)
                        Toast.makeText(this@MainActivity, "✅ متصل به ${best.remarks}", Toast.LENGTH_LONG).show()
                        webView.evaluateJavascript("onVpnConnected()", null)
                    } else {
                        Toast.makeText(this@MainActivity, "❌ خطا در وارد کردن کانفیگ", Toast.LENGTH_LONG).show()
                        webView.evaluateJavascript("onScanError('خطا در وارد کردن کانفیگ')", null)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "❌ خطا: ${e.message}", Toast.LENGTH_LONG).show()
                    webView.evaluateJavascript("onScanError('${e.message}')", null)
                }
            }
        }

        @JavascriptInterface
        fun onDisconnectClick() {
            CoreServiceManager.stopVService(this@MainActivity)
            webView.post {
                webView.evaluateJavascript("onVpnDisconnected()", null)
            }
        }
    }

    private suspend fun importVlessConfig(vlessLink: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val (count, _) = AngConfigManager.importBatchConfig(
                    vlessLink, MmkvManager.decodeSettings().subscriptionId, true
                )
                if (count > 0) {
                    val serverList = MmkvManager.decodeServerList(
                        MmkvManager.decodeSettings().subscriptionId
                    )
                    if (serverList.isNotEmpty()) {
                        MmkvManager.selectServer(serverList.first().id)
                    }
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }
}
