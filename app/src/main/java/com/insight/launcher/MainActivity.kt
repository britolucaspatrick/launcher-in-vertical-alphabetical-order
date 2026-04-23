package com.insight.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 5)

        loadBackgroundImage(recyclerView)

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val apps = getInstalledApps()
        val adapter = AppAdapter(
            apps,
            onAppClick = { app ->
                launchApp(app.packageName)
            },
            onAppLongClick = { app ->
                showAppOptionsDialog(app)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadBackgroundImage(recyclerView: RecyclerView) {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val imageUrl = "https://picsum.photos/1080/2160?random=$dayOfYear&nature"

        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                ) {
                    recyclerView.background = resource
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    recyclerView.background = placeholder
                }
            })
    }

    private fun showAppOptionsDialog(app: AppItem) {
        val options = arrayOf("Informações do app", "Desinstalar")
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAppInfo(app.packageName)
                    1 -> uninstallApp(app.packageName)
                }
            }
            .show()
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun getInstalledApps(): List<AppItem> {
        val pm = packageManager
        val apps = mutableListOf<AppItem>()
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (resolveInfo in resolveInfos) {
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val packageName = appInfo.packageName

            // Skip the launcher app itself
            if (packageName == this.packageName) {
                continue
            }

            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            apps.add(AppItem(label, icon, packageName))
        }
        return apps.sortedBy { it.label }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        }
    }
}