package com.tomsphone.feature.carer.screens

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "PhotoCaptureScreen"

/**
 * Full-screen camera capture for Emergency ID photo.
 * 
 * Uses front camera by default for ID/selfie photos.
 * Works within pinned mode since it's an embedded camera, not an external app.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoCaptureScreen(
    onPhotoCaptured: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    // Request permission on first composition
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        when {
            cameraPermissionState.status.isGranted -> {
                // Camera permission granted - show camera
                CameraPreviewContent(
                    context = context,
                    onPhotoCaptured = { photoUri ->
                        // Save URI to settings
                        viewModel.setUserPhotoUri(photoUri)
                        onPhotoCaptured(photoUri)
                    },
                    onCancel = onCancel
                )
            }
            cameraPermissionState.status.shouldShowRationale -> {
                // Permission denied but can ask again
                PermissionRationale(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                    onCancel = onCancel
                )
            }
            else -> {
                // Permission denied permanently or not yet requested
                PermissionDenied(onCancel = onCancel)
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    context: Context,
    onPhotoCaptured: (String) -> Unit,
    onCancel: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }
    
    // Create PreviewView once
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    
    // Bind camera once when composable enters composition
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // Preview use case
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                // Image capture use case
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture
                
                // Use rear camera - easier for carer to photograph the user
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
                
                isCameraReady = true
                Log.d(TAG, "Camera bound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay with instructions and buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with cancel button and instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Instructions
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Point camera at user's face",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Spacer for balance
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            // Face guide oval
            Spacer(modifier = Modifier.weight(1f))
            
            Box(
                modifier = Modifier
                    .size(200.dp, 260.dp)
                    .border(3.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Capture button - simple and clear
            Button(
                onClick = {
                    if (!isCapturing && isCameraReady && imageCapture != null) {
                        isCapturing = true
                        Log.d(TAG, "Capture button pressed, starting capture...")
                        capturePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onSuccess = { uri ->
                                Log.d(TAG, "Capture success, calling onPhotoCaptured")
                                isCapturing = false
                                onPhotoCaptured(uri)
                            },
                            onError = { e ->
                                isCapturing = false
                                Log.e(TAG, "Photo capture failed: ${e.message}", e)
                            }
                        )
                    } else {
                        Log.w(TAG, "Capture blocked: isCapturing=$isCapturing, cameraReady=$isCameraReady, imageCapture=${imageCapture != null}")
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCameraReady) Color.White else Color.Gray,
                    contentColor = Color.Black
                ),
                enabled = !isCapturing && isCameraReady
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.Black,
                        strokeWidth = 3.dp
                    )
                } else if (!isCameraReady) {
                    // Loading indicator while camera initializes
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    // Simple camera emoji
                    Text("📷", style = MaterialTheme.typography.headlineLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera Permission Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "The camera is needed to take an ID photo for emergency services. This photo helps EMTs verify the patient's identity.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow Camera")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onCancel) {
            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun PermissionDenied(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera Access Denied",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To take an ID photo, please enable camera access in your device settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go Back")
        }
    }
}

/**
 * Capture photo and save to app-private storage.
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val capture = imageCapture ?: run {
        onError(IllegalStateException("ImageCapture not initialized"))
        return
    }
    
    // Save to app-private files directory
    val photoFile = File(context.filesDir, "emergency_photo.jpg")
    
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    val executor: Executor = ContextCompat.getMainExecutor(context)
    
    capture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = photoFile.absolutePath
                Log.d(TAG, "Photo saved: $savedUri, exists=${photoFile.exists()}, size=${photoFile.length()}")
                onSuccess(savedUri)
            }
            
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture error: ${exception.message}", exception)
                onError(exception)
            }
        }
    )
}
