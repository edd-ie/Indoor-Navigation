package edd_ie.com.github.arnav


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.SharedCamera
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import edd_ie.com.github.arnav.databinding.ActivityMainBinding
import java.util.EnumSet

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding



    // requestInstall(Activity, true) will triggers installation of
    // Google Play Services for AR if necessary.
    private var userRequestedInstall = true
    private var session: Session? = null
    private var sharedSession: Session? = null
    private var sharedCamera: SharedCamera? = null
    private var cameraId: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        enableEdgeToEdge()
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }



    @SuppressLint("SetTextI18n")
    private fun arSupportCheck() {
        if(ArCoreApk.getInstance().checkAvailability(this).isSupported){
            binding.txt1.text = "Device is supported"
        }
        try {
            if (session == null) {
                when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        // Success: Safe to create the AR session.
                        session = Session(this)
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        // When this method returns `INSTALL_REQUESTED`:
                        // 1. ARCore pauses this activity.
                        // 2. ARCore prompts the user to install or update Google Play
                        //    Services for AR (market://details?id=com.google.ar.core).
                        // 3. ARCore downloads the latest device profile data.
                        // 4. ARCore resumes this activity. The next invocation of
                        //    requestInstall() will either return `INSTALLED` or throw an
                        //    exception if the installation or update did not succeed.
                        userRequestedInstall = false
                        return
                    }
                }
            }
        }catch(e:Exception){
            if (e is UnavailableDeviceNotCompatibleException) {
                // Handle the specific custom exception if needed
                Log.e("ARCore Unsupported", "Device is not supported", e)
                // Display an appropriate message to the user and return gracefully.
                Toast.makeText(this, "Device is not supported: $e", Toast.LENGTH_LONG)
                    .show()
                return
            } else {
                // Handle other exceptions
                Toast.makeText(this, "Error requesting ARCore install: $e", Toast.LENGTH_LONG)
                    .show()
                Log.e("ARCore", "Error requesting ARCore install", e)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        // Check camera permission.
        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        // Ensure that Google Play Services for AR and ARCore device profile data are
        // installed and up to date.
        // Enable AR-related functionality on ARCore supported devices only.
        arSupportCheck()


        // Create an ARCore session that supports camera sharing.
        sharedSession = Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA))

        // Store the ARCore shared camera reference.
        sharedCamera = sharedSession!!.sharedCamera

        // Store the ID of the camera that ARCore uses.
        cameraId = sharedSession!!.cameraConfig.cameraId


        //Initializing ar session
        createArSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
    }

    private fun createArSession() {
        // Create a session config.
        val config = Config(session)

        // Do feature-specific operations here, such as enabling depth or turning on
        // support for Augmented Faces.
        // Create a camera config filter for the session.

        val filter = CameraConfigFilter(session)

        // Return only camera configs that target 30 fps camera capture frame rate.
        filter.targetFps = EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30)

        // Get list of configs that match filter settings.
        // In this case, this list is guaranteed to contain at least one element,
        // because both TargetFps.TARGET_FPS_30 and DepthSensorUsage.DO_NOT_USE
        // are supported on all ARCore supported devices.
        val cameraConfigList = session?.getSupportedCameraConfigs(filter)
        if (cameraConfigList != null) {
            for(x in cameraConfigList){
                println(x)
            }
        }

        // Use element 0 from the list of returned camera configs. This is because
        // it contains the camera config that best matches the specified filter
        // settings.
        session?.cameraConfig = cameraConfigList?.get(0)!!


        // Configure the session.
        session?.configure(config)
    }
}


