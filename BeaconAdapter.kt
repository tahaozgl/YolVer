package com.example.viyo

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BeaconAdapter : RecyclerView.Adapter<BeaconAdapter.BeaconViewHolder>() {

    private val beaconList = ArrayList<BeaconItem>()

    fun updateList(newItem: BeaconItem) {
        val index = beaconList.indexOfFirst { it.macAddress == newItem.macAddress }
        if (index != -1) {
            beaconList[index] = newItem
            notifyItemChanged(index)
        } else {
            beaconList.add(newItem)
            notifyItemInserted(beaconList.size - 1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_beacon, parent, false)
        return BeaconViewHolder(view)
    }

    class BeaconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMac: TextView = view.findViewById(R.id.tvMacAddress)
        val txtInfo: TextView = view.findViewById(R.id.tvInfo)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        val item = beaconList[position]
        holder.txtMac.text = "[${item.type}] MAC: ${item.macAddress}"
        holder.txtInfo.text = "RSSI: ${item.rssi} | Tx: ${item.txPower} | ~${"%.2f".format(item.estimatedDistance)}m"
    }

    override fun getItemCount() = beaconList.size
}