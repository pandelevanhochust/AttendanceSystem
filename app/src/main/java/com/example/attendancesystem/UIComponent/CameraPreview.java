package com.example.attendancesystem.UIComponent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.FaceDetector;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.ViewGroup;

import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import android.content.Context;
import android.util.AttributeSet;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import androidx.lifecycle.LifecycleOwner;
import com.example.attendancesystem.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// SetUp Camera
public class CameraPreview extends FrameLayout {

    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis analysisUseCase;
    private Preview previewUseCase;
    private final String TAG = "CameraX";

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        LayoutInflater.from(context).inflate(R.layout.camera_preview_layout,this,true);
        previewView = findViewById(R.id.previewView);
        startCamera(context);
    }

    //startCamera
    private void startCamera(Context context){
        ListenableFuture <ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraSelector = new CameraSelector.Builder().requireLensFacing(DesignatedCamera).build();

        cameraProviderFuture.addListener(() -> {
            try {
                // Take out the frame
                cameraProvider = cameraProviderFuture.get();
                if(cameraProvider != null){
                    cameraProvider.unbindAll();
                    bindPreviewUseCase();
                    bindAnalysisUseCase();
                }

            }catch(Exception e){
                Log.e(TAG,"cameraProviderFuture Error",e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    //Binding Preview -> showcase Image
    private void bindPreviewUseCase(){
        if(cameraProvider == null) return;

        cameraProvider.unbind(previewUseCase);
        Preview.Builder builder = new Preview.Builder();
//        builder.setTargetResolution(new Size(640,480));
        builder.setTargetRotation(getRotate());

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try{
            cameraProvider.bindToLifecycle((LifecycleOwner) getContext(), cameraSelector, previewUseCase);
        }catch(Exception e){
            Log.e(TAG,"Error when binding preview",e);
        }
    }

    //Binding Analysis
    private void bindAnalysisUseCase(){
        if(cameraProvider == null) return;
        cameraProvider.unbind(previewUseCase);

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        //        builder.setTargetResolution(new Size(640,480));
        builder.setTargetRotation(getRotate());

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try{
            cameraProvider.bindToLifecycle((LifecycleOwner) getContext(), cameraSelector, analysisUseCase);
        }catch(Exception e){
            Log.e(TAG,"Error when binding preview",e);
        }
    }

    //Rotation
    private int getRotate() {
        return previewView.getDisplay() != null
                ? previewView.getDisplay().getRotation()
                : Surface.ROTATION_0;
    }

    private void analyze(@NonNull ImageProxy imageProxy){
        if(imageProxy.getImage() != null){
            InputImage inputImage = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            FaceDetector faceDetector = (FaceDetector) FaceDetection.getClient();

            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> onSuccessListener(faces, inputImage))
                    .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                    .addOnCompleteListener(task -> image.close());
        }else{
            imageProxy.close();
        }
    }

}
