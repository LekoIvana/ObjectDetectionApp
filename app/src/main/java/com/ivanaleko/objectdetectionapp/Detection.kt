package com.ivanaleko.objectdetectionapp

data class Detection(
    val className: String,
    val confidence: Float,
    val box: DetectionBox
)

data class DetectionBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)