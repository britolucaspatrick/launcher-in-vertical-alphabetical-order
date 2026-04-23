package com.insight.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
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

    companion object {
        private const val TAG = "LauncherDebug"
    }

    private val uninstallResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Uninstall result received. Result code: ${result.resultCode}, Data: ${result.data}")

        // Add a delay to allow system to update package information
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if the app was actually uninstalled
            lastUninstalledPackage?.let { packageName ->
                Log.d(TAG, "Checking if app $packageName was uninstalled")
                val isInstalled = isAppInstalled(packageName)
                Log.d(TAG, "App $packageName is installed: $isInstalled")

                if (!isInstalled) {
                    Log.d(TAG, "App was uninstalled, refreshing list")
                    refreshAppsList()
                } else {
                    Log.d(TAG, "App was not uninstalled, keeping list as is")
                }
            } ?: Log.d(TAG, "No package tracked for uninstall")

            lastUninstalledPackage = null
        }, 2000) // 2 second delay
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during app initialization: ${e.message}", e)
            showFrozenAppDialog()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during app initialization: ${e.message}", e)
            showGenericErrorDialog()
        }
    }

    private fun setupAppsList() {
        Log.d(TAG, "Setting up apps list")
        val apps = getInstalledApps()
        Log.d(TAG, "Loaded ${apps.size} apps")
        currentRecyclerAdapter = AppAdapter(
            apps,
            onAppClick = { app ->
                launchApp(app.packageName)
            },
            onAppLongClick = { app ->
                showAppOptionsDialog(app)
            }
        )
        recyclerView.adapter = currentRecyclerAdapter
    }

    private fun refreshAppsList() {
        Log.d(TAG, "Refreshing apps list")
        setupAppsList()
        showUninstalledPopup()
    }

    private fun showUninstalledPopup() {
        AlertDialog.Builder(this)
            .setTitle("App desinstalado")
            .setMessage("O app foi removido com sucesso.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    private fun canUninstallApp(packageName: String): Boolean {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val isSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val canUninstall = !isSystemApp
            Log.d(TAG, "App $packageName - isSystemApp: $isSystemApp, canUninstall: $canUninstall")
            canUninstall
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "App $packageName not found when checking if can uninstall")
            false
        }
    }

    private fun showAppOptionsDialog(app: AppItem) {
        val canUninstall = canUninstallApp(app.packageName)
        Log.d(TAG, "Showing options for ${app.label} (${app.packageName}) - canUninstall: $canUninstall")

        val optionsList = mutableListOf("Informações do app")
        if (canUninstall) {
            optionsList.add("Desinstalar")
        }

        val options = optionsList.toTypedArray()
        Log.d(TAG, "Options shown: ${options.joinToString()}")

        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                Log.d(TAG, "User selected option $which for ${app.label}")
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
        Log.d(TAG, "Starting uninstall process for $packageName")
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
        lastUninstalledPackage = packageName
        Log.d(TAG, "Launching uninstall intent for $packageName")
        uninstallResultLauncher.launch(intent)
    }

    private fun getInstalledApps(): List<AppItem> {
        val pm = packageManager
        val apps = mutableListOf<AppItem>()
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        Log.d(TAG, "Found ${resolveInfos.size} resolve infos")
        
        for (resolveInfo in resolveInfos) {
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val packageName = appInfo.packageName
            val label = pm.getApplicationLabel(appInfo).toString()

            // Skip the launcher app itself
            if (packageName == this.packageName) {
                Log.d(TAG, "Skipping launcher app: $packageName")
                continue
            }

            Log.d(TAG, "Adding app: $label ($packageName)")
            val icon = pm.getApplicationIcon(appInfo)
            apps.add(AppItem(label, icon, packageName))
        }
        
        val sortedApps = apps.sortedBy { it.label }
        Log.d(TAG, "Returning ${sortedApps.size} sorted apps")
        return sortedApps
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            // App is considered installed if we can retrieve its application info
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // App is not installed
            false
        }
    }

    private fun showFrozenAppDialog() {
        AlertDialog.Builder(this)
            .setTitle("App Congelado")
            .setMessage("Este app está congelado pelo sistema. Para usar o launcher, descongele o app nas configurações do dispositivo ou gerenciador de apps.")
            .setPositiveButton("Abrir Configurações") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open app settings: ${e.message}")
                }
                finish()
            }
            .setNegativeButton("Fechar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGenericErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("Erro")
            .setMessage("Ocorreu um erro inesperado ao iniciar o app. Tente reiniciar o dispositivo.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}