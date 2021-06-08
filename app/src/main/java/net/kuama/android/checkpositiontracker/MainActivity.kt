package net.kuama.android.checkpositiontracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import net.kuama.android.backgroundLocation.LocationRequestManager
import net.kuama.android.backgroundLocation.service.BackgroundService
import org.joda.time.DateTime

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPermission()
        //test
        val backgroundLocationIntent = Intent(this, BackgroundService::class.java)
        val startButton = findViewById<Button>(R.id.bt_location)
        startButton.setOnClickListener {
            // TODO Remember to ask for android.permission.ACCESS_FINE_LOCATION before starting the service
            val intent = BLService
                .Builder()
                .eta(DateTime.now().plusDays(4))
                .journey(
                    Journey(
                        listOf(
                            Position(12.0, 12.0),
                            Position(45.4064333, 11.87676),
                            Position(13.0, 12.0)
                        )
                    )
                )
                .build(this)
            startService(intent)
            startService(backgroundLocationIntent)
            val myBroadcastReceiver = BLService.LocationReceiver()
            val intentFilter = IntentFilter(BackgroundService::class.java.name)
            registerReceiver(myBroadcastReceiver, intentFilter)


            val trackerReceiver = TrackerReceiver()
            val trackerIntentFilter = IntentFilter(BLService::class.java.name)
            registerReceiver(trackerReceiver, trackerIntentFilter)
        }

        val stopButton = findViewById<Button>(R.id.bt_stop)
        stopButton.setOnClickListener {
            stopService(backgroundLocationIntent)
        }
    }


    /**
     * Get the user permissions to use the position
     */
    private fun getPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ), LocationRequestManager.REQUEST_ID
        )
    }

    fun checkPermission(): Boolean {

        return ActivityCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    /**
     * Check if the GPS services are active
     */
    private fun checkGPSActive(): Boolean {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

///test
class TrackerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val status: String? = intent?.status
        val position: Position? = intent?.position
        val dateTime: DateTime? = intent?.date
        if (position != null) {
            Log.d("Location", "Alert type: $status. Location: ${position.latitude}-${position.longitude}, Date: $dateTime")
        }
    }
}



