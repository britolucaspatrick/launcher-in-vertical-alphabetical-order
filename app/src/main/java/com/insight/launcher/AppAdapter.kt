package com.insight.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.insight.launcher.presentation.model.AppUiModel

class AppAdapter(
    private var apps: List<AppUiModel>,
    private val onAppClick: (AppUiModel) -> Unit,
    private val onAppLongClick: (AppUiModel) -> Unit,
    private var fontSize: Float,
    private var fontStyle: Int
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.label.text = app.label
        holder.icon.setImageDrawable(app.icon)

        holder.label.textSize = fontSize
        holder.label.setTypeface(null, fontStyle)

        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener {
            onAppLongClick(app)
            true
        }
    }

    override fun getItemCount() = apps.size

    fun updateApps(newApps: List<AppUiModel>) {
        this.apps = newApps
        notifyDataSetChanged()
    }

    fun updateStyles(size: Float, style: Int) {
        this.fontSize = size
        this.fontStyle = style
        notifyDataSetChanged()
    }
}
