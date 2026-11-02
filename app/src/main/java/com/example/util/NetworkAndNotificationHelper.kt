package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.MovioraQualityPreset
import com.example.data.local.UserClientSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class LiveNetworkStatus(
    val isConnected: Boolean = true,
    val transportLabelAr: String = "Wi-Fi",
    val isWifi: Boolean = true,
    val isCellular: Boolean = false,
    val downstreamKbps: Int = 15_000
) {
    fun recommendedMaxVideoHeight(settings: UserClientSettings): Int {
        val networkCeiling = when {
            !isConnected -> 2160 // offline local file
            isWifi -> settings.wifiDefaultMaxHeight
            isCellular -> settings.mobileDefaultMaxHeight
            else -> 1080
        }
        val bandwidthRecommended = when {
            downstreamKbps <= 0 -> 720
            downstreamKbps < 800 -> 240
            downstreamKbps < 1_500 -> 360
            downstreamKbps < 2_800 -> 480
            downstreamKbps < 5_500 -> 720
            downstreamKbps < 18_000 -> 1080
            downstreamKbps < 30_000 -> 1440
            else -> 2160
        }
        return when (settings.qualityPreset) {
            MovioraQualityPreset.AUTO -> minOf(networkCeiling, bandwidthRecommended)
            else -> minOf(networkCeiling, settings.qualityPreset.maxCapHeight)
        }
    }
}

object NetworkAndNotificationHelper {

    private const val CHANNEL_ID = "moviora_cinema_notifications"

    fun observeNetworkStatus(context: Context): Flow<LiveNetworkStatus> = callbackFlow {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun queryCurrent(): LiveNetworkStatus {
            val active = cm.activeNetwork ?: return LiveNetworkStatus(
                isConnected = false,
                transportLabelAr = "غير متصل",
                isWifi = false,
                isCellular = false,
                downstreamKbps = 0
            )
            val caps = cm.getNetworkCapabilities(active) ?: return LiveNetworkStatus(
                isConnected = false,
                transportLabelAr = "غير متصل",
                isWifi = false,
                isCellular = false,
                downstreamKbps = 0
            )
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val cell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val kbps = caps.linkDownstreamBandwidthKbps.coerceAtLeast(0)
            val label = when {
                !validated -> "غير متصل"
                wifi -> "Wi-Fi عالي السرعة"
                cell -> "بيانات الهاتف"
                else -> "متصل بالشبكة"
            }
            return LiveNetworkStatus(
                isConnected = validated,
                transportLabelAr = label,
                isWifi = wifi,
                isCellular = cell,
                downstreamKbps = kbps
            )
        }

        trySend(queryCurrent())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(queryCurrent())
            }

            override fun onLost(network: Network) {
                trySend(queryCurrent())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(queryCurrent())
            }
        }

        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { cm.registerNetworkCallback(req, callback) }

        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "إشعارات MOVIORA",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات الحلقات الجديدة، الأعمال التاريخية المضافة، وحالة اشتراك VIP"
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun postLocalNotificationIfPermitted(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
