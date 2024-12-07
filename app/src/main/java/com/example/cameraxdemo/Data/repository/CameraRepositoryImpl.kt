package com.example.cameraxdemo.Data.repository

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import com.example.cameraxdemo.Domain.repository.CameraRepository
import com.example.cameraxdemo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class CameraRepositoryImpl @Inject constructor(
    private val application: Application
) : CameraRepository{

    private var recording: Recording? = null
    override suspend fun takePhoto(controller: LifecycleCameraController) {
        controller.takePicture(
            ContextCompat.getMainExecutor(application),
            object : ImageCapture.OnImageCapturedCallback() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val matrix = Matrix().apply {
                        postRotate(
                            image.imageInfo.rotationDegrees.toFloat()
                        )
                    }
                    val imageBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        savePhoto(imageBitmap)
                    }
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun recordVideo(controller: LifecycleCameraController) {
        //if currently recording
        if(recording != null)
        {
            recording?.stop()
            recording = null
            return
        }
        val timeInMillis = System.currentTimeMillis()
        val file = File(
            application.filesDir,
            "${timeInMillis}_video"+".mp4"
        )
        recording = controller.startRecording(
            FileOutputOptions.Builder(file).build(),
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(application)
        ){event ->
                if(event is VideoRecordEvent.Finalize)
                {
                    if(event.hasError())
                    {
                        recording?.close()
                        recording = null
                    }else{
                       CoroutineScope(Dispatchers.IO).launch {
                           saveVideo(file)
                       }
                    }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun savePhoto(bitmap: Bitmap)
    {
        withContext(Dispatchers.IO)
        {
            val resolver: ContentResolver = application.contentResolver
            val imageCollection = MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
            val appName = application.getString(R.string.app_name)

            val timeInMillis = System.currentTimeMillis()
            // ContentValues() This class is used to store a set of values that the ContentResolver can process.
            val imageContentValues : ContentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "${timeInMillis}_photo"+".jpg"
                )
                put(
                    MediaStore.Images.Media.DATE_TAKEN,
                    timeInMillis
                )
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_DCIM + "/$appName"
                )
                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpg"
                )
                put(
                    MediaStore.Images.Media.IS_PENDING,
                    1
                )
            }

            val imageMediaStoreUri: Uri? = resolver.insert(
                imageCollection,  //Uri
                imageContentValues // ContentValues
            )

            imageMediaStoreUri?.let{ uri ->
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    imageContentValues.clear()
                    imageContentValues.put(
                        MediaStore.Images.Media.IS_PENDING,
                        0
                    )
                    resolver.update(uri, imageContentValues, null, null)
                }catch (e :Exception)
                {
                    e.printStackTrace()
                    resolver.delete(uri, null, null)
                }

            }

        }

    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveVideo(file:File)
    {
        withContext(Dispatchers.IO)
        {
            val resolver: ContentResolver = application.contentResolver
            val videoCollection = MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

            val appName = application.getString(R.string.app_name)
            val timeInMillis = System.currentTimeMillis()

            // ContentValues() This class is used to store a set of values that the ContentResolver can process.
            val videoContentValues : ContentValues = ContentValues().apply {
                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    file.name
                )
                put(
                    MediaStore.MediaColumns.DATE_TAKEN,
                    timeInMillis
                )
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DCIM + "/$appName"
                )
                put(
                    MediaStore.Video.Media.MIME_TYPE,
                    "video/mp4"
                )
                put(
                    MediaStore.Video.Media.IS_PENDING,
                    1
                )
                put(
                    MediaStore.MediaColumns.DATE_ADDED,
                    timeInMillis/1000
                )
                put(
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    timeInMillis/1000
                )
            }

            val videoMediaStoreUri: Uri? = resolver.insert(
                videoCollection,  //Uri
                videoContentValues // ContentValues
            )

            videoMediaStoreUri?.let{ uri ->
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        resolver.openInputStream(
                            Uri.fromFile(file)
                        )?.use{ inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    videoContentValues.clear()
                    videoContentValues.put(
                        MediaStore.Images.Media.IS_PENDING,
                        0
                    )
                    resolver.update(uri, videoContentValues, null, null)
                }catch (e :Exception)
                {
                    e.printStackTrace()
                    resolver.delete(uri, null, null)
                }

            }

        }
    }
}