val onStartCapture: () -> Unit = {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        val notificationGranted =
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (!notificationGranted) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
            return@setContent
        }
    }

    val captureIntent =
        projectionManager.createScreenCaptureIntent()

    screenCaptureLauncher.launch(captureIntent)
}

