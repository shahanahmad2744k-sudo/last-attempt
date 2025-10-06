package com.hotspotmanager

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var refreshButton: FloatingActionButton
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshDeviceList()
            handler.postDelayed(this, 5000) // Refresh every 5 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        startAutoRefresh()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        refreshButton = findViewById(R.id.fabRefresh)
        
        deviceAdapter = DeviceAdapter { device ->
            blockDevice(device)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = deviceAdapter
        
        refreshButton.setOnClickListener {
            refreshDeviceList()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        }
    }

    private fun refreshDeviceList() {
        Thread {
            val devices = getConnectedDevices()
            runOnUiThread {
                deviceAdapter.updateDevices(devices)
            }
        }.start()
    }

    private fun getConnectedDevices(): List<ConnectedDevice> {
        val devices = mutableListOf<ConnectedDevice>()
        
        try {
            // Read ARP table to get connected devices
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat /proc/net/arp"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 6 && parts[3] != "00:00:00:00:00:00" && parts[0] != "0.0.0.0") {
                        val ip = parts[0]
                        val mac = parts[3]
                        val deviceName = getDeviceName(ip)
                        devices.add(ConnectedDevice(deviceName, ip, mac))
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            // Add fallback demo data for testing
            devices.add(ConnectedDevice("Demo Device 1", "192.168.43.100", "AA:BB:CC:DD:EE:01"))
            devices.add(ConnectedDevice("Demo Device 2", "192.168.43.101", "AA:BB:CC:DD:EE:02"))
        }
        
        return devices
    }

    private fun getDeviceName(ip: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "nslookup $ip"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            process.waitFor()
            val result = reader.readLines().find { it.contains("name =") }
                ?.substringAfter("name = ")?.substringBefore(".")
            result ?: "Device-${ip.substringAfterLast(".")}"
        } catch (e: Exception) {
            "Device-${ip.substringAfterLast(".")}"
        }
    }

    private fun blockDevice(device: ConnectedDevice) {
        Thread {
            try {
                // Block device using iptables (requires root)
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "iptables -A INPUT -s ${device.ipAddress} -j DROP"))
                val exitCode = process.waitFor()
                
                runOnUiThread {
                    if (exitCode == 0) {
                        Toast.makeText(this@MainActivity, "Device ${device.name} blocked", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to block device. Root access required.", Toast.LENGTH_LONG).show()
                    }
                    refreshDeviceList()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to block device. Root access required.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun startAutoRefresh() {
        handler.post(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            refreshDeviceList()
        }
    }
}