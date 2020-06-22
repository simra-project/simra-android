package de.tuberlin.mcc.simra.app.services;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An {@link IntentService} subclass for handling asynchronous Camera requests in
 * a service on a separate handler thread.
 */
public class CameraService extends IntentService implements CameraXConfig.Provider {
    private static final String TAKE_PICTURE = "de.tuberlin.mcc.simra.app.services.action.TAKE_PICTURE";
    private static final String EXTRA_NAME = "de.tuberlin.mcc.simra.app.services.extra.NAME";
    private static final String EXTRA_PATH = "de.tuberlin.mcc.simra.app.services.extra.PATH";
    private static final Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;
    private CustomLifecycle lifecycle;

    public CameraService() {
        super("CameraService");
        lifecycle = new CustomLifecycle();
        lifecycle.doOnResume();
        lifecycle.doOnStart();
    }

    /**
     * Starts this service to take a Picture and save it with the given name. If
     * the service is already performing a task this action will be queued.
     *
     * @param context Context of the Service or Activity
     * @param name    Name of the File
     * @param path    Directory Path to save file in
     */
    public static void takePicture(Context context, String name, String path) {
        Intent intent = new Intent(context, CameraService.class);
        intent.setAction(TAKE_PICTURE);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_PATH, path);
        context.startService(intent);
    }

    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (TAKE_PICTURE.equals(action)) {
                final String name = intent.getStringExtra(EXTRA_NAME);
                final String path = intent.getStringExtra(EXTRA_PATH);
                handleTakePicture(name, path);
            }
        }
    }

    /**
     * Handle action TakePicture
     * <p>
     * First in main Thread, to get the Camera Provider
     * Then in Background thread to do File IO
     */
    public void handleTakePicture(String name, String path) {

        // TODO: Check Permissions here and at app start
        String[] requiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {

                final ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                final CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                final ImageCapture imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(Surface.ROTATION_0)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Camera has to be bound to the Lifecycle in order to be used later on (via ImageCapture)
                Camera camera = cameraProvider.bindToLifecycle(lifecycle, cameraSelector, imageCapture);

                executor.execute(() -> {
                    File file = new File(path, name + ".jpg");
                    ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

                    imageCapture.takePicture(outputFileOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            new Handler(Looper.getMainLooper()).post(() -> lifecycle.doOnDestroy());
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException error) {
                            Toast.makeText(getBaseContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                            error.printStackTrace();
                        }
                    });
                });
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                .setCameraExecutor(executor)
                .build();
    }

    public class CustomLifecycle implements LifecycleOwner {

        private LifecycleRegistry mLifecycleRegistry;

        CustomLifecycle() {
            mLifecycleRegistry = new LifecycleRegistry(this);
            mLifecycleRegistry.markState(Lifecycle.State.CREATED);
        }

        void doOnResume() {
            mLifecycleRegistry.markState(Lifecycle.State.RESUMED);
        }

        void doOnStart() {
            mLifecycleRegistry.markState(Lifecycle.State.STARTED);
        }

        void doOnDestroy() {
            mLifecycleRegistry.markState(Lifecycle.State.DESTROYED);
        }

        @NonNull
        public Lifecycle getLifecycle() {
            return mLifecycleRegistry;
        }
    }
}
