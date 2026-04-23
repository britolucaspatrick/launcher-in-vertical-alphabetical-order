package com.insight.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var currentRecyclerAdapter: AppAdapter
    private var lastUninstalledPackage: String? = null
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "LauncherDebug"
        const val PREFS_NAME = "LauncherPrefs"
        const val KEY_FONT_SIZE = "fontSize"
        const val KEY_FONT_STYLE = "fontStyle"
        const val DEFAULT_FONT_SIZE = 14f 
        const val DEFAULT_FONT_STYLE = Typeface.BOLD
    }

    private val uninstallResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Handler(Looper.getMainLooper()).postDelayed({
            lastUninstalledPackage?.let { packageName ->
                if (!isAppInstalled(packageName)) {
                    refreshAppsList()
                }
            }
            lastUninstalledPackage = null
        }, 2000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)

            recyclerView = findViewById(R.id.recyclerView)
            recyclerView.layoutManager = GridLayoutManager(this, 5)

            loadBackgroundImage(recyclerView)

            ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            setupAppsList()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
        }
    }

    private fun setupAppsList() {
        val apps = getInstalledApps()
        currentRecyclerAdapter = AppAdapter(
            apps,
            onAppClick = { app -> launchApp(app.packageName) },
            onAppLongClick = { app -> showAppOptionsDialog(app) },
            fontSize = getSavedFontSize(),
            fontStyle = getSavedFontStyle()
        )
        recyclerView.adapter = currentRecyclerAdapter
    }

    private fun refreshAppsList() {
        setupAppsList()
    }

    private fun loadBackgroundImage(recyclerView: RecyclerView) {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val imageUrl = "https://picsum.photos/1080/2160?random=$dayOfYear&nature"
        Glide.with(this).load(imageUrl).centerCrop().into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
            override fun onResourceReady(resource: android.graphics.drawable.Drawable, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?) {
                recyclerView.background = resource
            }
            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                recyclerView.background = placeholder
            }
        })
    }

    private fun showAppOptionsDialog(app: AppItem) {
        val canUninstall = canUninstallApp(app.packageName)
        val optionsList = mutableListOf("Informações do app", "Configurações do Launcher")
        if (canUninstall) optionsList.add("Desinstalar")

        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(optionsList.toTypedArray()) { _, which ->
                when (optionsList[which]) {
                    "Informações do app" -> openAppInfo(app.packageName)
                    "Configurações do Launcher" -> showLauncherSettingsDialog()
                    "Desinstalar" -> uninstallApp(app.packageName)
                }
            }
            .show()
    }

    private fun showLauncherSettingsDialog() {
        val sizes = arrayOf("10", "12", "14", "16", "18", "20")
        val styles = arrayOf("Normal", "Negrito", "Itálico", "Negrito Itálico")
        val styleValues = intArrayOf(Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Configurações de Texto")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val spinnerSize = view.findViewById<Spinner>(R.id.spinnerSize)
        val spinnerStyle = view.findViewById<Spinner>(R.id.spinnerStyle)

        spinnerSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizes)
        spinnerStyle.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, styles)

        // Set current values
        val currentSize = getSavedFontSize().toInt().toString()
        spinnerSize.setSelection(sizes.indexOf(currentSize).coerceAtLeast(0))
        val currentStyle = getSavedFontStyle()
        spinnerStyle.setSelection(styleValues.indexOf(currentStyle).coerceAtLeast(0))

        builder.setView(view)
        builder.setPositiveButton("Salvar") { _, _ ->
            val newSize = sizes[spinnerSize.selectedItemPosition].toFloat()
            val newStyle = styleValues[spinnerStyle.selectedItemPosition]
            saveFontSize(newSize)
            saveFontStyle(newStyle)
            refreshAppsList()
            Toast.makeText(this, "Configurações aplicadas", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun openAppInfo(packageName: String) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun uninstallApp(packageName: String) {
        lastUninstalledPackage = packageName
        uninstallResultLauncher.launch(Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun getInstalledApps(): List<AppItem> {
        val pm = packageManager
        val apps = mutableListOf<AppItem>()
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        
        for (resolveInfo in resolveInfos) {
            val appInfo = resolveInfo.activityInfo.applicationInfo
            if (appInfo.packageName == packageName) continue
            apps.add(AppItem(pm.getApplicationLabel(appInfo).toString(), pm.getApplicationIcon(appInfo), appInfo.packageName))
        }
        return apps.sortedBy { it.label }
    }

    private fun launchApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try { packageManager.getApplicationInfo(packageName, 0); true } catch (e: Exception) { false }
    }

    private fun canUninstallApp(packageName: String): Boolean {
        return try { (packageManager.getApplicationInfo(packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 } catch (e: Exception) { false }
    }

    fun saveFontSize(size: Float) = sharedPreferences.edit().putFloat(KEY_FONT_SIZE, size).apply()
    fun getSavedFontSize(): Float = sharedPreferences.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
    fun saveFontStyle(style: Int) = sharedPreferences.edit().putInt(KEY_FONT_STYLE, style).apply()
    fun getSavedFontStyle(): Int = sharedPreferences.getInt(KEY_FONT_STYLE, DEFAULT_FONT_STYLE)
}
