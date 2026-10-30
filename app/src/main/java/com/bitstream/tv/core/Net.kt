package com.bitstream.tv.core

import android.content.Context
import com.bitstream.tv.R
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Shared OkHttp client.
 *
 * Old Android 6/7 TV boxes often have a stale system CA store that predates
 * Let's Encrypt (ISRG Root X1) and Google Trust Services (GTS Root R4), which
 * makes every HTTPS call fail with a certificate error. We therefore verify
 * against the system store FIRST and fall back to the two bundled modern roots
 * (see res/raw). Verification stays strict: hostname checks and validity
 * periods still apply, we only accept a couple of additional trusted roots.
 */
object Net {

    @Volatile
    private var client: OkHttpClient? = null

    fun httpClient(context: Context): OkHttpClient =
        client ?: synchronized(this) {
            client ?: build(context.applicationContext).also { client = it }
        }

    private fun build(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .cache(Cache(File(context.cacheDir, "http"), 5L * 1024 * 1024))
        extraTrust(context)?.let { (factory, manager) ->
            builder.sslSocketFactory(factory, manager)
        }
        return builder.build()
    }

    private fun extraTrust(context: Context): Pair<SSLSocketFactory, X509TrustManager>? = try {
        val certFactory = CertificateFactory.getInstance("X.509")
        val extras = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
        intArrayOf(R.raw.isrgrootx1, R.raw.gtsr4).forEach { res ->
            context.resources.openRawResource(res).use { input ->
                extras.setCertificateEntry("root_$res", certFactory.generateCertificate(input))
            }
        }

        val systemTm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()

        val extraTms = TrustManagerFactory.getInstance("X509")
            .apply { init(extras) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()

        val composite = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                systemTm.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                try {
                    systemTm.checkServerTrusted(chain, authType)
                } catch (e: CertificateException) {
                    extraTms.forEach { it.checkServerTrusted(chain, authType) }
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = systemTm.acceptedIssuers
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(composite), SecureRandom())
        }
        sslContext.socketFactory to composite
    } catch (t: Throwable) {
        null // fall back to system trust only
    }
}
