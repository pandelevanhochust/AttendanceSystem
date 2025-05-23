package com.example.attendancesystem.UIComponent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.HardwareBufferRenderer;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import android.util.Pair;
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

import com.example.attendancesystem.FaceProcessor;
import com.google.mlkit.vision.common.InputImage;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import androidx.lifecycle.LifecycleOwner;
import com.example.attendancesystem.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private GraphicOverlay overlayView;
    private final String TAG = "CameraX";
    private Interpreter tfLite;

    private static final int INPUT_SIZE = 112;
    private static final int OUTPUT_SIZE = 112;
    private float[][] embeddings;

    private final HashMap<String,Faces.Recognition> savedFaces = new HashMap<>();

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        LayoutInflater.from(context).inflate(R.layout.camera_preview_layout,this,true);
        previewView = findViewById(R.id.previewView);

        overlayView = new GraphicOverlay(context,null);
        addView(overlayView);
        loadModel();
        loadFaces();
        startCamera();
    }

    //startCamera
    private void startCamera(){
        ListenableFuture <ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        // The variable store Camera input
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

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
        }, ContextCompat.getMainExecutor(getContext()));
    }

    //Binding Preview -> showcase Image
    private void bindPreviewUseCase(){
        if(cameraProvider == null) return;

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

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

    //Binding Analysis -> Analyse Image
    private void bindAnalysisUseCase(){
        if(cameraProvider == null) return;
        cameraProvider.unbind(previewUseCase);

        cameraExecutor = Executors.newSingleThreadExecutor();

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

    //Pass Image to ML Kit
    private void analyze(@NonNull ImageProxy imageProxy){
        if(imageProxy.getImage() != null){
            InputImage inputImage = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );
            // Initialize ML kit
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build();

            FaceDetector faceDetector = FaceDetection.getClient(options);

            // Detected Faces sent to preprocess
            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> onSuccessListener(faces, inputImage))
                    .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        }else{
            imageProxy.close();
        }
    }

    private void onSuccessListener(List<Face> faces, InputImage inputImage){
        Rect boundingBox = null;
        String name = null;
        float scaleX = (float) previewView.getWidth()/ (float) inputImage.getWidth();
        float scaleY = (float) previewView.getHeight()/ (float) inputImage.getHeight();

        if(!faces.isEmpty()){
            Face face = faces.get(0);
            boundingBox = face.getBoundingBox();
            overlayView.draw(boundingBox,scaleX,scaleY,"Detected Person");
            Image image = inputImage.getMediaImage();
            int rotation = inputImage.getRotationDegrees();

            Pair<String,Float> output  = recognize(image, rotation,boundingBox);
            if (output.second >= 1.00f){
                overlayView.draw(boundingBox, 1.0f, 1.0f, "unknown");
            }
            if(output.second < 1.00f) {
                overlayView.draw(boundingBox, 1.0f, 1.0f, output.first);
            }
            // need to add detection here
        }else{
            overlayView.draw(null,1.0f,1.0f,"unknown");
        }
    }

    private Pair<String,Float> recognize(Image image,int rotation, Rect boundingBox){
        String name = null;
        float min_distance = Float.MAX_VALUE;
        Pair <String, Float> ret = null;

        Bitmap bmp = FaceProcessor.ImgtoBmp(image,rotation,boundingBox);
        ByteBuffer input = FaceProcessor.convertBitmapToByteBuffer(bmp);

        Object[] inputArray = {input};
        Map<Integer,Object> outputMap = new HashMap<>();
        embeddings = new float[1][OUTPUT_SIZE];
        outputMap.put(0,embeddings);

        tfLite.runForMultipleInputsOutputs(inputArray,outputMap);

        if(!savedFaces.isEmpty()){
            for(HashMap.Entry<String,Faces.Recognition> entry : savedFaces.entrySet()){
                name = entry.getKey();
                final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];

                // Euclid distances between two embeddings
                float distance = 0;
                for (int i=0; i < embeddings[0].length; i++){
                    float diff = embeddings[0][i] - knownEmb[i];
                    distance += diff * diff;
                }
                min_distance = Math.min(distance,min_distance);

                //Can use Consine Similarity instead
            }
        }

        ret = new Pair<>(name,min_distance);
        return ret;
    }

    private void train(Image image){}


    //Loading model
    private void loadModel() {
        try {
            AssetFileDescriptor fileDescriptor = getContext().getAssets().openFd("mobile_face_net.tflite");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();

            MappedByteBuffer model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            tfLite = new Interpreter(model);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
        }
    }

    //Load savedFaces from Backend
    public void loadFaces(){}

}
