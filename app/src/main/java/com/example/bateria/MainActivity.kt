package com.example.bateria

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var batteryPercentTextView: TextView
    private lateinit var batteryVoltageTextView: TextView
    private lateinit var batteryTemperatureTextView: TextView
    private var batteryPercentage: Float = 0f
    private var batteryVoltage: Float = 0f
    private var batteryTemperature: Float = 0f
    private val CHANNEL_ID = "example_channel_id"
    private val NOTIFICATION_ID = 1
    private val CAMERA_REQUEST_CODE = 100
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isBlinking = false
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }


        val notifyButton = findViewById<Button>(R.id.button)
        createNotificationChannel(this)
        notifyButton.setOnClickListener {
            if (batteryPercentage < 20) {
                sendNotification1()
                startBlinking()
            } else {
                sendNotification2()
                stopBlinking()
            }
        }

        var mediaPlayer = MediaPlayer()
        mediaPlayer = MediaPlayer.create(this, R.raw.gravando)
        var mediaPlayer2 = MediaPlayer()
        mediaPlayer2 = MediaPlayer.create(this,R.raw.gravando2)

        batteryPercentTextView = findViewById(R.id.batteryPercentageTextView)
        batteryVoltageTextView = findViewById(R.id.batteryVoltageTextView)
        batteryTemperatureTextView = findViewById(R.id.batteryTemperatureTextView)
        val batteryStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    batteryPercentage = (level / scale.toFloat()) * 100

                    val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    batteryVoltage = voltage/1000.0f

                    val temperature = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                    batteryTemperature = temperature/10.0f



                    if (batteryPercentage > 99.99) {

                        mediaPlayer.start()
                    }
                    if (batteryPercentage < 20){
                        stopBlinking()
                        mediaPlayer2.start()
                                                }
                    batteryPercentTextView.text = "Bateria: %.2f%%".format(batteryPercentage)
                    batteryVoltageTextView.text = "Voltagem: %.2fV".format(batteryVoltage)
                    batteryTemperatureTextView.text = "Temperatura: %.1f°C".format(batteryTemperature)

                }
            }
        }
        registerReceiver(batteryStatusReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "example_channelId"
            val descriptionText = "this is an exaple channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText

            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification1() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                                                    )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.sembateria)
            .setContentTitle("BATERIA FRACA!")
            .setContentText("CONECTE SEU CELULAR NA TOMADA!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)



        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun sendNotification2() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.bateriacheia)
            .setContentTitle("A BATERIA NÂO ESTÀ FRACA!")
            .setContentText("NÂO É PRECISO CONECTAR NA TOMADA!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão concedida", Toast.LENGTH_SHORT).show()


            }
        }
    }

    private fun blinkFlashLight() {
        if (isBlinking) {
            val handler = Handler()
            val blinkDelay: Long = 500

            handler.post(object : Runnable {
                var on = false
                override fun run() {
                    if (isBlinking) {
                    on = !on
                    setFlashlight(on)
                    handler.postDelayed(this, blinkDelay)
                }
            }
        })
    }
}
    private fun setFlashlight(status: Boolean){
        try {
            cameraManager.setTorchMode(cameraId ?: return, status)
            } catch (e: Exception){e.printStackTrace()}
                                                }

    private fun startBlinking(){
        isBlinking = true
        blinkFlashLight()
                                }
    private fun stopBlinking(){
        isBlinking = false
        setFlashlight(false)
        handler.removeCallbacksAndMessages(null)
                                }

}

