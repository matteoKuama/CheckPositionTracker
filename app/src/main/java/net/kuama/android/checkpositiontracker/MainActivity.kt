package net.kuama.android.checkpositiontracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
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
import java.time.Duration
import java.time.LocalDateTime

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
                    .eta(DateTime.now())
                    .journey(Journey(listOf(Position(12.0, 12.0), Position(13.0, 12.0), Position(45.4064333, 11.87676))))
                    .build(this)
                startService(intent)
                startService(backgroundLocationIntent)
                val myBroadcastReceiver = BLService.LocationReceiver()
                val intentFilter = IntentFilter(BackgroundService::class.java.name)
                registerReceiver(myBroadcastReceiver, intentFilter)
//                startService(Intent(this, CheckPositionService::class.java))


//                val myBroadcastReceiver = CheckPositionTracker(
//                    MyTravel(
//                        TravelStatus.traveling,
//                        Position(12.0, 12.0),
//                        null,
//                        LocalDateTime.of(2022, 12, 12, 12, 12)
//                    ),
//                    Route(
//                        listOf(Position(12.0, 12.0), Position(13.0, 12.0)),
//                        Duration.ofDays(13),
//                        RouteMode.cycling
//                    ),
//                    applicationContext.getSystemService(
//                        BATTERY_SERVICE
//                    ) as BatteryManager
//                )
//                val intentFilter = IntentFilter(BackgroundService::class.java.name)
//                registerReceiver(myBroadcastReceiver, intentFilter)
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
class MyLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val position:Position? = intent?.position
        if (position != null) {
            Log.d("Location", "${position.latitude}-${position.longitude}")
        }
    }
}



