package net.kuama.android.checkpositiontracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.kuama.android.backgroundLocation.service.BackgroundService
import net.kuama.android.backgroundLocation.LocationStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(this, BackgroundService::class.java))
        val myBroadcastReceiver = MyLocationReceiver()
        val intentFilter = IntentFilter(BackgroundService::class.java.name)
        registerReceiver(myBroadcastReceiver, intentFilter)
    }
}

