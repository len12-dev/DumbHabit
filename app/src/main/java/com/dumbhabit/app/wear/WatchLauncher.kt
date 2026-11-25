package com.dumbhabit.app.wear

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.Wearable

/** Ouvre l'app montre depuis le téléphone (quand une routine démarre). */
object WatchLauncher {

    private val watchActivity = ComponentName(
        "len.studio.dumbhabit",
        "com.dumbhabit.wear.WearActivity",
    )

    fun openWatchApp(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN).setComponent(watchActivity)
        val helper = RemoteActivityHelper(context)
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node -> helper.startRemoteActivity(intent, node.id) }
        }
    }
}
