package com.ivanaleko.objectdetectionapp

import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

@OptIn(TransformExperimental::class)
@Composable
fun CameraScreen(
    cameraExecutor: ExecutorService,
    webSocketManager: WebSocketManager,
    detectionText: String,
    detections: List<Detection>
) {

    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner

    val purple = Color(0xFF8B3DFF)
    val lightPurple = Color(0xFFB56CFF)

    val mainExecutor = remember {
        Executor { command ->
            Handler(
                Looper.getMainLooper()
            ).post(command)
        }
    }

    var transformationMatrix by remember {
        mutableStateOf<Matrix?>(null)
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    var imageAnalysis by remember {
        mutableStateOf<ImageAnalysis?>(null)
    }

    DisposableEffect(Unit) {

        onDispose {

            try {

                imageAnalysis?.clearAnalyzer()

                if (cameraProviderFuture.isDone) {

                    cameraProviderFuture
                        .get()
                        .unbindAll()
                }

                Log.d(
                    "CameraX",
                    "Kamera i ImageAnalysis zaustavljeni"
                )

            } catch (exception: Exception) {

                Log.e(
                    "CameraX",
                    "Greška pri zaustavljanju kamere",
                    exception
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(
            modifier =
                Modifier.fillMaxSize(),

            factory = { androidContext ->

                val previewView =
                    PreviewView(
                        androidContext
                    ).apply {

                        scaleType =
                            PreviewView.ScaleType.FIT_CENTER
                    }

                cameraProviderFuture
                    .addListener({

                        try {

                            val cameraProvider =
                                cameraProviderFuture
                                    .get()

                            val preview =
                                Preview.Builder()
                                    .build()
                                    .also {
                                            cameraPreview ->

                                        cameraPreview
                                            .surfaceProvider =
                                            previewView
                                                .surfaceProvider
                                    }

                            val currentImageAnalysis =
                                ImageAnalysis
                                    .Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis
                                            .STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .build()

                            imageAnalysis =
                                currentImageAnalysis

                            val transformFactory =
                                ImageProxyTransformFactory()
                                    .apply {

                                        setUsingCropRect(
                                            false
                                        )

                                        setUsingRotationDegrees(
                                            false
                                        )
                                    }

                            currentImageAnalysis
                                .setAnalyzer(
                                    cameraExecutor
                                ) { imageProxy ->

                                    try {

                                        Log.d(
                                            "CameraFrame",
                                            "Frame: " +
                                                    "${imageProxy.width}x${imageProxy.height}, " +
                                                    "rotacija=" +
                                                    "${imageProxy.imageInfo.rotationDegrees}"
                                        )

                                        val sourceTransform =
                                            transformFactory
                                                .getOutputTransform(
                                                    imageProxy
                                                )

                                        previewView.post {

                                            try {

                                                val targetTransform =
                                                    previewView
                                                        .outputTransform

                                                if (
                                                    targetTransform != null
                                                ) {

                                                    val coordinateTransform =
                                                        CoordinateTransform(
                                                            sourceTransform,
                                                            targetTransform
                                                        )

                                                    val matrix =
                                                        Matrix()

                                                    coordinateTransform
                                                        .transform(
                                                            matrix
                                                        )

                                                    transformationMatrix =
                                                        Matrix(
                                                            matrix
                                                        )
                                                }

                                            } catch (
                                                exception: Exception
                                            ) {

                                                Log.e(
                                                    "CameraTransform",
                                                    "Greška pri transformaciji koordinata",
                                                    exception
                                                )
                                            }
                                        }

                                        val jpegBytes =
                                            imageProxyToJpeg(
                                                imageProxy
                                            )

                                        if (
                                            jpegBytes != null
                                        ) {

                                            val sent =
                                                webSocketManager
                                                    .sendFrame(
                                                        jpegBytes
                                                    )

                                            if (sent) {

                                                Log.d(
                                                    "CameraFrame",
                                                    "JPEG frame poslan: " +
                                                            "${jpegBytes.size} bajtova"
                                                )

                                            } else {

                                                Log.d(
                                                    "CameraFrame",
                                                    "Čeka se odgovor servera"
                                                )
                                            }
                                        }

                                    } catch (
                                        exception: Exception
                                    ) {

                                        Log.e(
                                            "CameraFrame",
                                            "Greška pri obradi framea",
                                            exception
                                        )

                                    } finally {

                                        imageProxy.close()
                                    }
                                }

                            val cameraSelector =
                                CameraSelector
                                    .DEFAULT_BACK_CAMERA

                            try {

                                cameraProvider
                                    .unbindAll()

                                cameraProvider
                                    .bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        currentImageAnalysis
                                    )

                                Log.d(
                                    "CameraX",
                                    "Kamera uspješno pokrenuta"
                                )

                            } catch (
                                exception: Exception
                            ) {

                                Log.e(
                                    "CameraX",
                                    "Pokretanje kamere nije uspjelo",
                                    exception
                                )
                            }

                        } catch (
                            exception: Exception
                        ) {

                            Log.e(
                                "CameraX",
                                "CameraProvider nije dostupan",
                                exception
                            )
                        }

                    }, mainExecutor)

                previewView
            }
        )

        DetectionOverlay(
            detections = detections,
            transformationMatrix =
                transformationMatrix,
            modifier =
                Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(
                    Alignment.TopCenter
                )
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 18.dp
                )
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(
                                    alpha = 0.78f
                                ),
                                Color(
                                    0xFF12081F
                                ).copy(
                                    alpha = 0.82f
                                )
                            )
                        ),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        )
                )
                .border(
                    width = 1.dp,
                    color =
                        purple.copy(
                            alpha = 0.60f
                        ),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        )
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        7.dp
                    )
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .padding(
                                end = 9.dp
                            )
                            .background(
                                color =
                                    lightPurple,
                                shape =
                                    RoundedCornerShape(
                                        50
                                    )
                            )
                            .padding(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "Detekcija aktivna",
                        color =
                            lightPurple,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Text(
                    text =
                        detectionText,
                    color =
                        Color.White,
                    fontSize =
                        16.sp,
                    lineHeight =
                        22.sp
                )
            }
        }
    }
}