package com.chartboost.sdk.internal.Networking

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chartboost.sdk.internal.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class CBImageDownloader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val urlFactory: (String) -> URL = { URL(it) },
    private val bitmapFactory: (InputStream) -> Bitmap? = { BitmapFactory.decodeStream(it) },
) {
    private val loadTimeout = 1000L // 1 second

    suspend fun downloadImage(imageUrl: String): Bitmap? =
        withContext(ioDispatcher) {
            var infoIconBitmap: Bitmap? = null
            var connection: HttpsURLConnection? = null
            var inputStream: InputStream? = null

            try {
                val url = urlFactory(imageUrl)
                withTimeout(loadTimeout) {
                    connection =
                        (url.openConnection() as HttpsURLConnection).apply {
                            doInput = true
                            inputStream = this.inputStream
                        }
                    infoIconBitmap = inputStream?.let {
                        bitmapFactory(it)
                    } ?: throw IOException("Bitmap decoded to null")
                }
            } catch (e: Exception) {
                Logger.w("Unable to download the info icon image", e)
            } finally {
                inputStream?.close()
                connection?.disconnect()
            }
            infoIconBitmap
        }
}
