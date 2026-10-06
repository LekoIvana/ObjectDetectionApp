package com.ivanaleko.objectdetectionapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun HomeScreen(
    onStartDetection: () -> Unit
) {

    val purple = Color(0xFF8B3DFF)
    val lightPurple = Color(0xFFB56CFF)

    val backgroundGradient =
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050509),
                Color(0xFF0A0711),
                Color(0xFF12081F),
                Color(0xFF09050F)
            )
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x558B3DFF),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 28.dp,
                    vertical = 40.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(75.dp)
            )


            DetectionLogo(
                color = purple
            )

            Spacer(
                modifier = Modifier.height(34.dp)
            )


            Text(
                text = "Object\nDetection",
                color = Color.White,
                fontSize = 46.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )


            Box(
                modifier = Modifier
                    .size(
                        width = 70.dp,
                        height = 3.dp
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                lightPurple,
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Text(
                text = "Real-time detekcija objekata",
                color = Color(0xFFC5C0CE),
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )


            Button(
                onClick = onStartDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        0.dp
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8B2EFF),
                                    Color(0xFF6B20E8),
                                    Color(0xFF4716A8)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "POKRENI DETEKCIJU",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )


            Text(
                text =
                    "Primjena modela strojnog učenja\n" +
                            "za prepoznavanje objekata",
                color = Color(0xFF96909F),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )
        }
    }
}



@Composable
private fun DetectionLogo(
    color: Color
) {

    Canvas(
        modifier = Modifier.size(125.dp)
    ) {


        val cornerStrokeWidth = 11f
        val squareStrokeWidth = 8f
        val xStrokeWidth = 9f

        val cornerLength = size.width * 0.24f
        val margin = size.width * 0.08f


        drawLine(
            color = color,
            start = Offset(
                margin,
                margin + cornerLength
            ),
            end = Offset(
                margin,
                margin
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                margin,
                margin
            ),
            end = Offset(
                margin + cornerLength,
                margin
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )


        drawLine(
            color = color,
            start = Offset(
                size.width - margin - cornerLength,
                margin
            ),
            end = Offset(
                size.width - margin,
                margin
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                size.width - margin,
                margin
            ),
            end = Offset(
                size.width - margin,
                margin + cornerLength
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )


        drawLine(
            color = color,
            start = Offset(
                margin,
                size.height - margin - cornerLength
            ),
            end = Offset(
                margin,
                size.height - margin
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                margin,
                size.height - margin
            ),
            end = Offset(
                margin + cornerLength,
                size.height - margin
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )


        drawLine(
            color = color,
            start = Offset(
                size.width - margin - cornerLength,
                size.height - margin
            ),
            end = Offset(
                size.width - margin,
                size.height - margin
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                size.width - margin,
                size.height - margin
            ),
            end = Offset(
                size.width - margin,
                size.height - margin - cornerLength
            ),
            strokeWidth = cornerStrokeWidth,
            cap = StrokeCap.Round
        )


        val objectSize = size.width * 0.34f

        val objectLeft =
            (size.width - objectSize) / 2f

        val objectTop =
            (size.height - objectSize) / 2f

        drawRect(
            color = color,
            topLeft = Offset(
                x = objectLeft,
                y = objectTop
            ),
            size = Size(
                objectSize,
                objectSize
            ),
            style = Stroke(
                width = squareStrokeWidth
            )
        )


        val xPadding = objectSize * 0.22f

        drawLine(
            color = color,
            start = Offset(
                x = objectLeft + xPadding,
                y = objectTop + xPadding
            ),
            end = Offset(
                x = objectLeft + objectSize - xPadding,
                y = objectTop + objectSize - xPadding
            ),
            strokeWidth = xStrokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(
                x = objectLeft + objectSize - xPadding,
                y = objectTop + xPadding
            ),
            end = Offset(
                x = objectLeft + xPadding,
                y = objectTop + objectSize - xPadding
            ),
            strokeWidth = xStrokeWidth,
            cap = StrokeCap.Round
        )
    }
}