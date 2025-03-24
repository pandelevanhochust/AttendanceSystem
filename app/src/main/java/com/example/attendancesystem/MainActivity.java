package com.example.attendancesystem;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;

import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.attendancesystem.UIComponent.CameraPreview;


public class MainActivity extends AppCompatActivity {
//    private CameraPreview cameraPreview = new CameraPreview();
    private static final String TAG = "MainActivity";
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_REQUEST_CODE = 1001;

    private FrameLayout cameraContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        cameraContainer = findViewById(R.id.camera_container);
    }

    @Override
    protected void onResume(){
        super.onResume();
        checkPermission();
    }

    private void checkPermission(){
        if(ContextCompat.checkSelfPermission(this,CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED){
            setupCamera();
        }else{
            ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION},CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupCamera(){
        CameraPreview cameraPreview = new CameraPreview(this, null);
        cameraContainer.removeAllViews();
        cameraContainer.addView(cameraPreview);
    }
}