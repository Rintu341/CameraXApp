package com.example.cameraxdemo

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxdemo.presentation.CameraScreen
import com.example.cameraxdemo.ui.theme.CameraXDEMOTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!hasPermissionGranted())
        {
            ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                0
            )
        }
        enableEdgeToEdge()
        setContent {
            CameraXDEMOTheme {
                CameraScreen(activity = this)
            }
        }
    }
    fun hasPermissionGranted() : Boolean
    {
        return CAMERA_PERMISSION.all{
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        val CAMERA_PERMISSION = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }
}

