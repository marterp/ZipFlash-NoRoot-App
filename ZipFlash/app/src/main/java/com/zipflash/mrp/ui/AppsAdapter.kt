package com.zipflash.mrp.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zipflash.mrp.R
import com.zipflash.mrp.models.AppInfo

class AppsAdapter(private val context: Context, private val appList: MutableList<AppInfo>) :
    RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    private val filteredList = mutableListOf<AppInfo>().also { it.addAll(appList) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredList[position]

        if (app.isActivity()) {
            holder.appName.text = "${app.name} · Activity"
        } else {
            holder.appName.text = app.name
        }

        holder.appIcon.setImageDrawable(app.icon)

        holder.itemView.setOnClickListener {
            try {
                if (app.isActivity()) {
                    val intent = Intent()
                    intent.setClassName(app.packageName, app.activityName!!)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = filteredList.size

    fun filter(query: String?) {
        filteredList.clear()
        if (query.isNullOrEmpty()) {
            filteredList.addAll(appList)
        } else {
            for (app in appList) {
                if (app.name.contains(query, ignoreCase = true)) {
                    filteredList.add(app)
                }
            }
        }
        notifyDataSetChanged()
    }

    fun updateData(newList: List<AppInfo>) {
        appList.clear()
        appList.addAll(newList)
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
    }
}
