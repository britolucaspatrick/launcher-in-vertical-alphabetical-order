package com.insight.launcher

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
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
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.facebook.shimmer.ShimmerFrameLayout
import com.insight.launcher.data.ImageCacheManager
import com.insight.launcher.data.repository.AppRepositoryImpl
import com.insight.launcher.domain.usecase.GetInstalledAppsUseCase
import com.insight.launcher.presentation.MainViewModel
import com.insight.launcher.presentation.MainViewModelFactory
import com.insight.launcher.presentation.model.AppUiModel

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var backgroundImageView: ImageView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var currentRecyclerAdapter: AppAdapter
    private var lastUninstalledPackage: String? = null
    private val imageCacheManager by lazy { ImageCacheManager(this) }
    
    private val repository by lazy { AppRepositoryImpl(this) }
    private val getInstalledAppsUseCase by lazy { GetInstalledAppsUseCase(repository) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application, getInstalledAppsUseCase)
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                loadBackgroundImage(true)
            }
        }
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
            backgroundImageView = findViewById(R.id.backgroundImageView)
            shimmerContainer = findViewById(R.id.shimmer_view_container)
            
            recyclerView.layoutManager = GridLayoutManager(this, 5)

            loadBackgroundImage()

            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenOffReceiver, filter)

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOffReceiver)
    }

    private fun setupRecyclerView() {
        currentRecyclerAdapter = AppAdapter(
            emptyList(),
            onAppClick = { app -> launchApp(app.packageName) }
        ) { app ->
            showAppOptionsDialog(app)
        }
        recyclerView.adapter = currentRecyclerAdapter
    }

    private fun observeViewModel() {
        viewModel.apps.observe(this) { apps ->
            currentRecyclerAdapter.updateApps(apps)
        }
    }

    private fun loadBackgroundImage(isRotation: Boolean = false) {
        val base64Image = if (isRotation) {
            imageCacheManager.rotateImages()
        } else {
            imageCacheManager.getCurrentBase64() ?: imageCacheManager.rotateImages()
        }

        if (base64Image != null) {
            shimmerContainer.stopShimmer()
            shimmerContainer.visibility = View.GONE
            
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val requestOptions = RequestOptions()
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()

                Glide.with(this)
                    .load(imageBytes)
                    .apply(requestOptions)
                    .into(backgroundImageView)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding base64 image", e)
            }
            
            if (isRotation || imageCacheManager.getNextBase64() == null) {
                imageCacheManager.prefetchNextImage()
            }
            return
        }

        backgroundImageView.setImageDrawable(null)
        shimmerContainer.startShimmer()
        shimmerContainer.visibility = View.VISIBLE
        
        imageCacheManager.prefetchNextImage { success ->
            if (success) {
                runOnUiThread { loadBackgroundImage(true) }
            } else {
                runOnUiThread {
                    shimmerContainer.stopShimmer()
                    shimmerContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun showAppOptionsDialog(app: AppUiModel) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_app_options)
        
        val window = dialog.window
        window?.let {
            it.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            val params = it.attributes
            params.gravity = Gravity.BOTTOM
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.y = 100 // Distância do fundo
            it.attributes = params
        }

        val txtTitle = dialog.findViewById<TextView>(R.id.dialogTitle)
        val btnInfo = dialog.findViewById<TextView>(R.id.btnInfo)
        val btnUninstall = dialog.findViewById<TextView>(R.id.btnUninstall)
        val divider = dialog.findViewById<View>(R.id.divider)

        txtTitle.text = app.label
        
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

        dialog.show()
    }

    private fun openAppInfo(packageName: String) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
        )
    }

    private fun uninstallApp(packageName: String) {
        lastUninstalledPackage = packageName
        uninstallResultLauncher.launch(
            Intent(Intent.ACTION_DELETE).apply {
                data = "package:$packageName".toUri()
            }
        )
    }

    private fun launchApp(packageName: String) {
        executeLaunch(packageName)
    }

    private fun executeLaunch(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }
}
