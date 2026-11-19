package com.amdg.jajanyuks.ui.buyer

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amdg.jajanyuks.MainActivity
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.data.OrderRepository
import com.amdg.jajanyuks.domain.Order
import com.amdg.jajanyuks.domain.OrderStatus
import com.amdg.jajanyuks.domain.newlyReady
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// Shows "your order is ready" when one of the signed-in user's orders turns READY.
// ponytail: listens only while the app process is alive (open or recently in the background);
// a push when the app is fully closed needs FCM + Cloud Functions (Blaze billing plan).
class OrderReadyNotifier(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val orders: OrderRepository,
) {
    private val scope = MainScope()
    private var job: Job? = null

    fun start() {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.order_ready_channel))
                .build(),
        )
        // Runs on sign-in and sign-out: restart the watcher for the new user (or stop it).
        auth.addAuthStateListener { a ->
            job?.cancel()
            val uid = a.currentUser?.uid ?: return@addAuthStateListener
            job = scope.launch {
                var before: Map<String, OrderStatus>? = null
                orders.ordersForBuyer(uid).collect { list ->
                    newlyReady(before, list).forEach(::notify)
                    before = list.associate { it.id to it.status }
                }
            }
        }
    }

    private fun notify(order: Order) {
        // Android 13+ asks the user first (requested on the buyer map screen).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.order_ready_title))
            .setContentText(context.getString(R.string.order_ready_text, order.vendorName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(order.id.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "order_ready"
    }
}
