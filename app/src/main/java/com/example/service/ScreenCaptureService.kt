package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenCaptureService : Service() {

    companion object {

        private const val TAG = "QtexScreenCapture"

        private const val CHANNEL_ID =
            "QtexScreenCaptureChannel"

        private const val NOTIFICATION_ID = 1001

        const val ACTION_START =
            "com.example.service.ACTION_START"

        const val ACTION_STOP =
            "com.example.service.ACTION_STOP"

        const val EXTRA_RESULT_CODE =
            "EXTRA_RESULT_CODE"

        const val EXTRA_RESULT_DATA =
            "EXTRA_RESULT_DATA"

        private val _latestFrame =
            MutableStateFlow<Bitmap?>(null)

        val latestFrame: StateFlow<Bitmap?> =
            _latestFrame.asStateFlow()

        private val _isCapturing =
            MutableStateFlow(false)

        val isCapturing: StateFlow<Boolean> =
            _isCapturing.asStateFlow()
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService {
            return this@ScreenCaptureService
        }
    }

    private val binder = LocalBinder()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projectionCallback: MediaProjection.Callback? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        Log.d(TAG, "Service created")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {

                val resultCode =
                    intent.getIntExtra(
                        EXTRA_RESULT_CODE,
                        -1
                    )

                val resultData =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA,
                            Intent::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Intent>(
                            EXTRA_RESULT_DATA
                        )
                    }

                if (
                    resultCode != -1 &&
                    resultData != null
                ) {

                    startForegroundServiceNotification()

                    startCapture(
                        resultCode,
                        resultData
                    )

                } else {

                    Log.e(
                        TAG,
                        "Invalid MediaProjection permission"
                    )

                    stopSelf()
                }
            }

            ACTION_STOP -> {

                stopCapture()

                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceNotification() {

        val notification: Notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "Qtex AI Signal Analyzer"
                )
                .setContentText(
                    "Screen chart analysis is active"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_camera
                )
                .setOngoing(true)
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .build()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun startCapture(
        resultCode: Int,
        permissionData: Intent
    ) {

        try {

            /*
             * Always clean the previous session first.
             */
            stopCaptureInternal()

            val projectionManager =
                getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            mediaProjection =
                projectionManager.getMediaProjection(
                    resultCode,
                    permissionData
                )

            if (mediaProjection == null) {

                Log.e(
                    TAG,
                    "MediaProjection is null"
                )

                _isCapturing.value = false

                return
            }

            projectionCallback =
                object : MediaProjection.Callback() {

                    override fun onStop() {

                        Log.d(
                            TAG,
                            "MediaProjection stopped"
                        )

                        stopCaptureInternal()
                    }
                }

            mediaProjection?.registerCallback(
                projectionCallback!!,
                null
            )

            val windowManager =
                getSystemService(
                    Context.WINDOW_SERVICE
                ) as WindowManager

            val metrics =
                DisplayMetrics()

            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
                .getRealMetrics(metrics)

            /*
             * Half-resolution capture.
             *
             * This is much lighter on the phone
             * than capturing the complete screen.
             */
            val width =
                (metrics.widthPixels / 2)
                    .coerceAtLeast(360)

            val height =
                (metrics.heightPixels / 2)
                    .coerceAtLeast(640)

            val density =
                metrics.densityDpi

            imageReader =
                ImageReader.newInstance(
                    width,
                    height,
                    PixelFormat.RGBA_8888,
                    2
                )

            imageReader?.setOnImageAvailableListener(
                { reader ->

                    processLatestImage(
                        reader,
                        width,
                        height
                    )

                },
                null
            )

            virtualDisplay =
                mediaProjection?.createVirtualDisplay(
                    "QtexAIAnalyzer",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    null
                )

            if (virtualDisplay == null) {

                Log.e(
                    TAG,
                    "VirtualDisplay creation failed"
                )

                stopCaptureInternal()

                return
            }

            _isCapturing.value = true

            Log.d(
                TAG,
                "Capture started: ${width}x$height"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Capture start failed",
                e
            )

            stopCaptureInternal()
        }
    }

    private fun processLatestImage(
        reader: ImageReader,
        width: Int,
        height: Int
    ) {

        val image =
            try {
                reader.acquireLatestImage()
            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Image acquisition failed",
                    e
                )

                null
            }

        if (image == null) {
            return
        }

        try {

            val plane =
                image.planes.firstOrNull()

            if (plane == null) {
                return
            }

            val buffer =
                plane.buffer

            val pixelStride =
                plane.pixelStride

            val rowStride =
                plane.rowStride

            if (
                pixelStride <= 0 ||
                rowStride <= 0
            ) {
                return
            }

            val rowPadding =
                rowStride -
                    pixelStride * width

            val bitmapWidth =
                width +
                    rowPadding / pixelStride

            val bitmap =
                Bitmap.createBitmap(
                    bitmapWidth,
                    height,
                    Bitmap.Config.ARGB_8888
                )

            buffer.rewind()

            bitmap.copyPixelsFromBuffer(
                buffer
            )

            val frame =
                if (bitmapWidth != width) {

                    val cropped =
                        Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            width,
                            height
                        )

                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }

                    cropped

                } else {

                    bitmap
                }

            /*
             * Replace the previous frame safely.
             */
            val previous =
                _latestFrame.value

            _latestFrame.value = frame

            /*
             * Release old frame.
             */
            if (
                previous != null &&
                previous !== frame &&
                !previous.isRecycled
            ) {

                try {
                    previous.recycle()
                } catch (_: Exception) {
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Frame processing failed",
                e
            )

        } finally {

            image.close()
        }
    }

    private fun stopCapture() {

        try {

            mediaProjection?.let { projection ->

                projectionCallback?.let { callback ->

                    try {
                        projection.unregisterCallback(
                            callback
                        )
                    } catch (_: Exception) {
                    }
                }

                try {
                    projection.stop()
                } catch (_: Exception) {
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Projection stop error",
                e
            )
        }

        stopCaptureInternal()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

        } else {

            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun stopCaptureInternal() {

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }

        virtualDisplay = null

        try {
            imageReader?.setOnImageAvailableListener(
                null,
                null
            )
        } catch (_: Exception) {
        }

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }

        imageReader = null

        projectionCallback = null
        mediaProjection = null

        val previous =
            _latestFrame.value

        _latestFrame.value = null

        if (
            previous != null &&
            !previous.isRecycled
        ) {

            try {
                previous.recycle()
            } catch (_: Exception) {
            }
        }

        _isCapturing.value = false

        Log.d(
            TAG,
            "Capture stopped"
        )
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Qtex Screen Capture",
                    NotificationManager
                        .IMPORTANCE_LOW
                ).apply {

                    description =
                        "Shows when Qtex chart analysis is active"

                    setShowBadge(false)
                }

            val manager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(
                channel
            )
        }
    }

    override fun onDestroy() {

        stopCapture()

        super.onDestroy()
    }
}
