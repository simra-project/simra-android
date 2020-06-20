package de.tuberlin.mcc.simra.app.services;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
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
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class CameraService extends IntentService implements CameraXConfig.Provider {
    private static final String TAKE_PICTURE = "de.tuberlin.mcc.simra.app.services.action.FOO";
    private static final String EXTRA_NAME = "de.tuberlin.mcc.simra.app.services.extra.PARAM1";

    CustomLifecycle lifecycle;
    private Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;

    public CameraService() {
        super("CameraService");
        // TODO: Check Permissions here and at app start
        String[] requiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        lifecycle = new CustomLifecycle();
        lifecycle.doOnResume();
        lifecycle.doOnStart();
    }

    /**
     * Starts this service to take a Picture with the given name. If
     * the service is already performing a task this action will be queued.
     */
    // TODO: Customize helper method
    public static void takePicture(Context context, String name) {
        Intent intent = new Intent(context, CameraService.class);
        intent.setAction(TAKE_PICTURE);
        intent.putExtra(EXTRA_NAME, name);
        context.startService(intent);
    }

    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (TAKE_PICTURE.equals(action)) {
                final String name = intent.getStringExtra(EXTRA_NAME);
                handleTakePicture(name);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    public void handleTakePicture(String name) {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                ImageCapture.Builder builder = new ImageCapture.Builder();

                final ImageCapture imageCapture = builder
                        .setTargetRotation(Surface.ROTATION_0)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Camera has to be bound to the Lifecycle in order to be used later on (via ImageCapture)
                Camera camera = cameraProvider.bindToLifecycle(lifecycle, cameraSelector, imageCapture);

                File file = new File(getBatchDirectoryName(), name + ".jpg");

                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

                imageCapture.takePicture(outputFileOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                String msg = "Picture saved";
                                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                                lifecycle.doOnDestroy();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        Toast.makeText(getBaseContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }
                });

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }


    public String getBatchDirectoryName() {

        String app_folder_path = "";
        app_folder_path = Environment.getExternalStorageDirectory().toString() + "/images";
        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {

        }

        return app_folder_path;
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
