package com.hotspotmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val onBlockClick: (ConnectedDevice) -> Unit) : 
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    
    private var devices = listOf<ConnectedDevice>()

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.tvDeviceName)
        val ipAddress: TextView = view.findViewById(R.id.tvIpAddress)
        val macAddress: TextView = view.findViewById(R.id.tvMacAddress)
        val blockButton: Button = view.findViewById(R.id.btnBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.name
        holder.ipAddress.text = "IP: ${device.ipAddress}"
        holder.macAddress.text = "MAC: ${device.macAddress}"
        holder.blockButton.setOnClickListener { onBlockClick(device) }
    }

    override fun getItemCount() = devices.size

    fun updateDevices(newDevices: List<ConnectedDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}