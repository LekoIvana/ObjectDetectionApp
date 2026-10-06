package com.ivanaleko.objectdetectionapp

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class WebSocketManager(
    private val onResultReceived: (String) -> Unit
) {

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(
                10,
                TimeUnit.SECONDS
            )
            .readTimeout(
                0,
                TimeUnit.MILLISECONDS
            )
            .pingInterval(
                20,
                TimeUnit.SECONDS
            )
            .build()

    private var webSocket: WebSocket? = null

    private val isConnected =
        AtomicBoolean(false)

    private val waitingForResponse =
        AtomicBoolean(false)

    private val frameSentTime =
        AtomicLong(0L)

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val manualClose =
        AtomicBoolean(false)

    private val reconnectScheduled =
        AtomicBoolean(false)

    private val reconnectDelay =
        3000L

    private val responseTimeout =
        5000L

    private val serverUrl =
        "ws://192.168.2.60:8000/ws/detect"

    private val responseTimeoutRunnable =
        Runnable {

            if (
                waitingForResponse.compareAndSet(
                    true,
                    false
                )
            ) {

                frameSentTime.set(0L)

                Log.e(
                    "WebSocket",
                    "Odgovor servera nije stigao unutar 5 sekundi"
                )
            }
        }

    fun connect() {

        if (isConnected.get()) {

            Log.d(
                "WebSocket",
                "Veza je već aktivna"
            )

            return
        }

        manualClose.set(false)

        reconnectScheduled.set(false)

        Log.d(
            "WebSocket",
            "Pokušaj povezivanja sa serverom..."
        )

        val request =
            Request.Builder()
                .url(serverUrl)
                .build()

        webSocket =
            client.newWebSocket(
                request,
                createWebSocketListener()
            )
    }

    private fun createWebSocketListener():
            WebSocketListener {

        return object :
            WebSocketListener() {

            override fun onOpen(
                webSocket: WebSocket,
                response: Response
            ) {

                mainHandler.removeCallbacks(
                    responseTimeoutRunnable
                )

                isConnected.set(true)

                waitingForResponse.set(false)

                frameSentTime.set(0L)

                reconnectScheduled.set(false)

                Log.d(
                    "WebSocket",
                    "Veza sa serverom je otvorena"
                )
            }

            override fun onMessage(
                webSocket: WebSocket,
                text: String
            ) {

                mainHandler.removeCallbacks(
                    responseTimeoutRunnable
                )

                val sentTime =
                    frameSentTime.get()

                if (sentTime > 0L) {

                    val latency =
                        SystemClock
                            .elapsedRealtime() -
                                sentTime

                    Log.d(
                        "Latency",
                        "Ukupni latency: $latency ms"
                    )
                }

                frameSentTime.set(0L)

                waitingForResponse.set(false)

                Log.d(
                    "DetectionResult",
                    "Primljen rezultat: $text"
                )

                onResultReceived(
                    text
                )
            }

            override fun onMessage(
                webSocket: WebSocket,
                bytes: ByteString
            ) {

                mainHandler.removeCallbacks(
                    responseTimeoutRunnable
                )

                frameSentTime.set(0L)

                waitingForResponse.set(false)

                Log.d(
                    "WebSocket",
                    "Primljeni binarni podaci: " +
                            "${bytes.size} bajtova"
                )
            }

            override fun onFailure(
                webSocket: WebSocket,
                throwable: Throwable,
                response: Response?
            ) {

                mainHandler.removeCallbacks(
                    responseTimeoutRunnable
                )

                isConnected.set(false)

                waitingForResponse.set(false)

                frameSentTime.set(0L)

                Log.e(
                    "WebSocket",
                    "Greška veze: " +
                            "${throwable.message}",
                    throwable
                )

                scheduleReconnect()
            }

            override fun onClosing(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {

                mainHandler.removeCallbacks(
                    responseTimeoutRunnable
                )

                isConnected.set(false)

                waitingForResponse.set(false)

                frameSentTime.set(0L)

                Log.d(
                    "WebSocket",
                    "Server zatvara vezu: " +
                            "$code, $reason"
                )

                webSocket.close(
                    code,
                    reason
                )
            }

            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {

                mainHandler.removeCallbacks(
                    responseTimeoutRunnable
                )

                isConnected.set(false)

                waitingForResponse.set(false)

                frameSentTime.set(0L)

                Log.d(
                    "WebSocket",
                    "Veza zatvorena: " +
                            "$code, $reason"
                )

                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {

        if (manualClose.get()) {

            Log.d(
                "WebSocket",
                "Reconnect preskočen jer je veza ručno zatvorena"
            )

            return
        }

        if (
            !reconnectScheduled
                .compareAndSet(
                    false,
                    true
                )
        ) {

            return
        }

        Log.d(
            "WebSocket",
            "Ponovni pokušaj povezivanja za " +
                    "${reconnectDelay / 1000} sekunde"
        )

        mainHandler.postDelayed(
            {

                reconnectScheduled.set(false)

                if (
                    !manualClose.get() &&
                    !isConnected.get()
                ) {

                    connect()
                }

            },
            reconnectDelay
        )
    }

    fun sendFrame(
        jpegBytes: ByteArray
    ): Boolean {

        if (!isConnected.get()) {

            Log.d(
                "WebSocket",
                "Frame nije poslan jer WebSocket nije povezan"
            )

            return false
        }

        if (
            !waitingForResponse
                .compareAndSet(
                    false,
                    true
                )
        ) {

            return false
        }

        frameSentTime.set(
            SystemClock.elapsedRealtime()
        )

        val sent =
            webSocket?.send(
                ByteString.of(
                    *jpegBytes
                )
            ) ?: false

        if (!sent) {

            mainHandler.removeCallbacks(
                responseTimeoutRunnable
            )

            waitingForResponse.set(false)

            frameSentTime.set(0L)

            Log.e(
                "WebSocket",
                "Frame nije poslan"
            )

        } else {

            mainHandler.removeCallbacks(
                responseTimeoutRunnable
            )

            mainHandler.postDelayed(
                responseTimeoutRunnable,
                responseTimeout
            )

            Log.d(
                "WebSocket",
                "JPEG frame poslan: " +
                        "${jpegBytes.size} bajtova"
            )
        }

        return sent
    }

    fun close() {

        manualClose.set(true)

        reconnectScheduled.set(false)

        mainHandler.removeCallbacksAndMessages(
            null
        )

        isConnected.set(false)

        waitingForResponse.set(false)

        frameSentTime.set(0L)

        webSocket?.close(
            1000,
            "Aplikacija zatvorila vezu"
        )

        webSocket = null

        Log.d(
            "WebSocket",
            "WebSocket veza ručno zatvorena"
        )
    }
}