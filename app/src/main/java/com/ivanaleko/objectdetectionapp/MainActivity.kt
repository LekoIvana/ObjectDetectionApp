package com.ivanaleko.objectdetectionapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ivanaleko.objectdetectionapp.ui.theme.ObjectDetectionAppTheme
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webSocketManager: WebSocketManager

    private var showCamera by mutableStateOf(false)

    private var detectionText by mutableStateOf(
        "Čekam rezultat detekcije..."
    )

    private var detections by mutableStateOf<List<Detection>>(
        emptyList()
    )

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permissionGranted ->

            if (permissionGranted) {

                startDetection()

            } else {

                Toast.makeText(
                    this,
                    "Dozvola za kameru je potrebna.",
                    Toast.LENGTH_LONG
                ).show()

                showCamera = false
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        cameraExecutor =
            Executors.newSingleThreadExecutor()

        webSocketManager =
            WebSocketManager { result ->

                runOnUiThread {

                    Log.d(
                        "MainActivity",
                        "Rezultat za prikaz: $result"
                    )

                    parseDetectionResult(
                        result
                    )
                }
            }

        setContent {

            ObjectDetectionAppTheme {

                BackHandler(
                    enabled = showCamera
                ) {

                    closeCameraScreen()
                }

                if (showCamera) {

                    CameraScreen(
                        cameraExecutor = cameraExecutor,
                        webSocketManager = webSocketManager,
                        detectionText = detectionText,
                        detections = detections
                    )

                } else {

                    HomeScreen(
                        onStartDetection = {

                            openCameraScreen()
                        }
                    )
                }
            }
        }
    }

    private fun openCameraScreen() {

        val cameraPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (cameraPermissionGranted) {

            startDetection()

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun startDetection() {

        detectionText =
            "Čekam rezultat detekcije..."

        detections =
            emptyList()

        webSocketManager.connect()

        showCamera = true
    }

    private fun closeCameraScreen() {

        webSocketManager.close()

        detections =
            emptyList()

        detectionText =
            "Čekam rezultat detekcije..."

        showCamera = false
    }

    private fun parseDetectionResult(
        result: String
    ) {

        try {

            val jsonObject =
                JSONObject(result)

            if (jsonObject.has("error")) {

                detections =
                    emptyList()

                detectionText =
                    "Greška servera: ${
                        jsonObject.optString(
                            "error"
                        )
                    }"

                return
            }

            val numberOfDetections =
                jsonObject.optInt(
                    "num_detections",
                    0
                )

            val inferenceTime =
                jsonObject.optDouble(
                    "inference_time_ms",
                    0.0
                )

            val formattedInferenceTime =
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    inferenceTime
                )

            val detectionArray =
                jsonObject.optJSONArray(
                    "detections"
                )

            val parsedDetections =
                mutableListOf<Detection>()

            if (detectionArray != null) {

                for (
                index in
                0 until detectionArray.length()
                ) {

                    val detectionObject =
                        detectionArray
                            .getJSONObject(
                                index
                            )

                    val boxObject =
                        detectionObject
                            .getJSONObject(
                                "box"
                            )

                    val detection =
                        Detection(
                            className =
                                detectionObject
                                    .optString(
                                        "class",
                                        "unknown"
                                    ),

                            confidence =
                                detectionObject
                                    .optDouble(
                                        "confidence",
                                        0.0
                                    )
                                    .toFloat(),

                            box =
                                DetectionBox(

                                    x1 =
                                        boxObject
                                            .optDouble(
                                                "x1",
                                                0.0
                                            )
                                            .toFloat(),

                                    y1 =
                                        boxObject
                                            .optDouble(
                                                "y1",
                                                0.0
                                            )
                                            .toFloat(),

                                    x2 =
                                        boxObject
                                            .optDouble(
                                                "x2",
                                                0.0
                                            )
                                            .toFloat(),

                                    y2 =
                                        boxObject
                                            .optDouble(
                                                "y2",
                                                0.0
                                            )
                                            .toFloat()
                                )
                        )

                    parsedDetections.add(
                        detection
                    )
                }
            }

            detections =
                parsedDetections

            detectionText =
                "Objekti: $numberOfDetections\n" +
                        "Vrijeme: $formattedInferenceTime ms"

        } catch (exception: Exception) {

            detections =
                emptyList()

            detectionText =
                "Neispravan odgovor servera"

            Log.e(
                "MainActivity",
                "Greška pri čitanju JSON odgovora",
                exception
            )
        }
    }

    override fun onStart() {

        super.onStart()

        if (
            ::webSocketManager.isInitialized &&
            showCamera
        ) {

            webSocketManager.connect()
        }
    }

    override fun onStop() {

        if (
            ::webSocketManager.isInitialized &&
            showCamera
        ) {

            webSocketManager.close()
        }

        super.onStop()
    }

    override fun onDestroy() {

        if (::cameraExecutor.isInitialized) {

            cameraExecutor.shutdown()
        }

        if (::webSocketManager.isInitialized) {

            webSocketManager.close()
        }

        super.onDestroy()
    }
}

fun imageProxyToJpeg(
    imageProxy: ImageProxy
): ByteArray? {

    if (
        imageProxy.format !=
        ImageFormat.YUV_420_888
    ) {

        Log.e(
            "CameraFrame",
            "Nepodržan format slike: ${imageProxy.format}"
        )

        return null
    }

    val nv21Bytes =
        yuv420ToNv21(
            imageProxy
        )

    val yuvImage =
        YuvImage(
            nv21Bytes,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

    val outputStream =
        ByteArrayOutputStream()

    val compressed =
        yuvImage.compressToJpeg(

            Rect(
                0,
                0,
                imageProxy.width,
                imageProxy.height
            ),

            70,

            outputStream
        )

    if (!compressed) {

        Log.e(
            "CameraFrame",
            "Pretvaranje framea u JPEG nije uspjelo"
        )

        return null
    }

    return outputStream
        .toByteArray()
}

fun yuv420ToNv21(
    imageProxy: ImageProxy
): ByteArray {

    val width =
        imageProxy.width

    val height =
        imageProxy.height

    val ySize =
        width * height

    val uvSize =
        width * height / 4

    val nv21 =
        ByteArray(
            ySize +
                    uvSize * 2
        )

    val yPlane =
        imageProxy.planes[0]

    val uPlane =
        imageProxy.planes[1]

    val vPlane =
        imageProxy.planes[2]

    var position = 0

    for (
    row in
    0 until height
    ) {

        val rowStart =
            row *
                    yPlane.rowStride

        for (
        column in
        0 until width
        ) {

            nv21[position++] =
                yPlane.buffer.get(

                    rowStart +
                            column *
                            yPlane.pixelStride
                )
        }
    }

    for (
    row in
    0 until height / 2
    ) {

        val uRowStart =
            row *
                    uPlane.rowStride

        val vRowStart =
            row *
                    vPlane.rowStride

        for (
        column in
        0 until width / 2
        ) {

            nv21[position++] =
                vPlane.buffer.get(

                    vRowStart +
                            column *
                            vPlane.pixelStride
                )

            nv21[position++] =
                uPlane.buffer.get(

                    uRowStart +
                            column *
                            uPlane.pixelStride
                )
        }
    }

    return nv21
}