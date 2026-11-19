package com.paracare.tracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.paracare.tracker.util.AlertWorker
import com.paracare.tracker.util.NotificationHelper
import java.util.concurrent.TimeUnit

class ParaCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        // Vérification périodique en arrière-plan (péremption, PAO, stock critique, commandes)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "paracare_alerts",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES).build()
        )
    }
}
