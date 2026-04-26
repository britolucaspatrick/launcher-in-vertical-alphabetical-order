package com.insight.launcher

import android.app.Dialog
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.insight.launcher.data.ImageCacheManager
import com.insight.launcher.data.repository.AppRepositoryImpl
import com.insight.launcher.databinding.ActivityMainBinding
import com.insight.launcher.databinding.DialogAppOptionsBinding
import com.insight.launcher.domain.usecase.GetInstalledAppsUseCase
import com.insight.launcher.presentation.BackgroundState
import com.insight.launcher.presentation.MainViewModel
import com.insight.launcher.presentation.MainViewModelFactory
import com.insight.launcher.presentation.model.AppUiModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appAdapter: AppAdapter
    private var lastUninstalledPackage: String? = null

    private val repository by lazy { AppRepositoryImpl(this) }
    private val imageCacheManager by lazy { ImageCacheManager(this) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            application,
            GetInstalledAppsUseCase(repository),
            imageCacheManager
        )
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.loadBackground(isRotation = true)
            }
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.fetchApps()
        }
    }

    private val uninstallResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
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
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupWindowInsets()
            setupRecyclerView()
            observeViewModel()

            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenOffReceiver, filter)

            val packageFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            registerReceiver(packageReceiver, packageFilter)

            viewModel.fetchApps()
            viewModel.loadBackground()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers", e)
        }
    }

    private fun setupWindowInsets() {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarBackground) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.layoutParams.height = systemBars.top
            v.requestLayout()
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (8 * resources.displayMetrics.density).toInt()
            v.setPadding(
                systemBars.left,
                systemBars.top + extraTopPadding,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            emptyList(),
            onAppClick = { app -> executeLaunch(app.packageName) },
            onAppLongClick = { app -> showAppOptionsDialog(app) },
        )
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = appAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.apps.observe(this) { apps ->
            appAdapter.updateApps(apps)
        }

        viewModel.backgroundState.observe(this) { state ->
            handleBackgroundState(state)
        }
    }

    private fun handleBackgroundState(state: BackgroundState) {
        when (state) {
            is BackgroundState.Loading -> {
                binding.shimmerViewContainer.startShimmer()
                binding.shimmerViewContainer.visibility = View.VISIBLE
            }
            is BackgroundState.Success -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
                displayBackgroundImage(state.base64)
                if (state.isRotation) {
                    syncSystemWallpaper(state.base64)
                }
            }
            is BackgroundState.Error -> {
                binding.shimmerViewContainer.stopShimmer()
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun displayBackgroundImage(base64Image: String) {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val requestOptions = RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()

            Glide.with(this)
                .load(imageBytes)
                .apply(requestOptions)
                .into(binding.backgroundImageView)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding base64 image", e)
        }
    }

    private fun syncSystemWallpaper(base64Image: String) {
        Thread {
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val wallpaperManager = WallpaperManager.getInstance(this)
                bitmap?.let {
                    wallpaperManager.setBitmap(
                        it,
                        null,
                        true,
                        WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing system wallpaper", e)
            }
        }.start()
    }

    private fun showAppOptionsDialog(app: AppUiModel) {
        val dialog = Dialog(this)
        val dialogBinding = DialogAppOptionsBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                width = WindowManager.LayoutParams.MATCH_PARENT
                y = 100
            }
        }

        dialogBinding.apply {
            dialogTitle.text = app.label
            val canUninstall = repository.canUninstallApp(app.packageName)
            if (!canUninstall) {
                btnUninstall.visibility = View.GONE
                divider.visibility = View.GONE
            }

            btnInfo.setOnClickListener {
                openAppInfo(app.packageName)
                dialog.dismiss()
            }

            btnUninstall.setOnClickListener {
                uninstallApp(app.packageName)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun openAppInfo(packageName: String) {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app info", e)
        }
    }

    private fun uninstallApp(packageName: String) {
        lastUninstalledPackage = packageName
        try {
            uninstallResultLauncher.launch(
                Intent(Intent.ACTION_DELETE).apply {
                    data = "package:$packageName".toUri()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error launching uninstall", e)
        }
    }

    private fun executeLaunch(packageName: String) {
        try {
            packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app", e)
        }
    }

    companion object {
        private const val TAG = "LauncherDebug"
    }
}
