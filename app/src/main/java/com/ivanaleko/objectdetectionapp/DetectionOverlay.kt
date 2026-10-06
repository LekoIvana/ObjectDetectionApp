package com.ivanaleko.objectdetectionapp

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    transformationMatrix: Matrix?,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {

        val matrix = transformationMatrix ?: return@Canvas

        detections.forEach { detection ->


            val rect = RectF(
                detection.box.x1,
                detection.box.y1,
                detection.box.x2,
                detection.box.y2
            )
            android.util.Log.d(
                "BBoxScreen",
                "Prije zaslona: x1=${rect.left}, y1=${rect.top}, x2=${rect.right}, y2=${rect.bottom}"
            )

            matrix.mapRect(rect)

            android.util.Log.d(
                "BBoxScreen",
                "Na zaslonu: left=${rect.left}, top=${rect.top}, right=${rect.right}, bottom=${rect.bottom}"
            )

            if (
                rect.width() <= 1f ||
                rect.height() <= 1f
            ) {
                return@forEach
            }


            val left =
                rect.left.coerceIn(
                    0f,
                    size.width
                )

            val top =
                rect.top.coerceIn(
                    0f,
                    size.height
                )

            val right =
                rect.right.coerceIn(
                    0f,
                    size.width
                )

            val bottom =
                rect.bottom.coerceIn(
                    0f,
                    size.height
                )

            if (
                right <= left ||
                bottom <= top
            ) {
                return@forEach
            }


            val purpleColor =
                Color(0xFF8B3DFF)

            drawRect(
                color = purpleColor,
                topLeft = Offset(
                    x = left,
                    y = top
                ),
                size = Size(
                    width = right - left,
                    height = bottom - top
                ),
                style = Stroke(
                    width = 6f
                )
            )


            val confidencePercent =
                (detection.confidence * 100)
                    .toInt()

            val label =
                "${detection.className} $confidencePercent%"

            val textPaint =
                Paint().apply {

                    color =
                        android.graphics.Color.rgb(
                            139,
                            61,
                            255
                        )

                    textSize = 40f

                    isAntiAlias = true

                    isFakeBoldText = true

                    style =
                        Paint.Style.FILL
                }


            val textX =
                left.coerceAtLeast(8f)

            val textY =
                if (top > 50f) {
                    top - 10f
                } else {
                    top + 45f
                }

            drawContext
                .canvas
                .nativeCanvas
                .drawText(
                    label,
                    textX,
                    textY,
                    textPaint
                )
        }
    }
}