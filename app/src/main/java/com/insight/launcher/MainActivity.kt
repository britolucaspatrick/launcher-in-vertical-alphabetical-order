package com.insight.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.insight.launcher.data.repository.AppRepositoryImpl
import com.insight.launcher.domain.usecase.GetInstalledAppsUseCase
import com.insight.launcher.presentation.MainViewModel
import com.insight.launcher.presentation.MainViewModelFactory
import com.insight.launcher.presentation.model.AppUiModel
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var currentRecyclerAdapter: AppAdapter
    private var lastUninstalledPackage: String? = null
    
    private val repository by lazy { AppRepositoryImpl(this) }
    private val getInstalledAppsUseCase by lazy { GetInstalledAppsUseCase(repository) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application, getInstalledAppsUseCase)
    }

    companion object {
        private const val TAG = "LauncherDebug"
    }

    private val uninstallResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        Handler(Looper.getMainLooper()).postDelayed({
            lastUninstalledPackage?.let { packageName ->
                if (!repository.isAppInstalled(packageName)) {
                    viewModel.fetchApps()
                }
            }
            lastUninstalledPackage = null
        }, 2000)
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

            setupRecyclerView()
            observeViewModel()
            
            viewModel.fetchApps()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
        }
    }

    private fun setupRecyclerView() {
        currentRecyclerAdapter = AppAdapter(
            emptyList(),
            onAppClick = { app -> launchApp(app.packageName) },
            onAppLongClick = { app -> showAppOptionsDialog(app) }
        )
        recyclerView.adapter = currentRecyclerAdapter
    }

    private fun observeViewModel() {
        viewModel.apps.observe(this) { apps ->
            currentRecyclerAdapter.updateApps(apps)
        }
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

    private fun showAppOptionsDialog(app: AppUiModel) {
        val canUninstall = repository.canUninstallApp(app.packageName)
        val optionsList = mutableListOf("Informações do app")
        if (canUninstall) optionsList.add("Desinstalar")

        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(optionsList.toTypedArray()) { _, which ->
                when (optionsList[which]) {
                    "Informações do app" -> openAppInfo(app.packageName)
                    "Desinstalar" -> uninstallApp(app.packageName)
                }
            }
            .show()
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

    private fun launchApp(packageName: String) {
        executeLaunch(packageName)
    }

    private fun executeLaunch(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }
}
