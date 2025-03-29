package com.example.attendancesystem;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.attendancesystem.UIComponent.CameraPreview;
import com.example.attendancesystem.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_REQUEST_CODE = 1001;

    private FrameLayout cameraContainer;

    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //Binding data
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnIdCard = findViewById(R.id.btn_id_card);
        ImageButton btnAdd = findViewById(R.id.btn_add);
        ImageButton btnCamera = findViewById(R.id.btn_camera);

//        btnIdCard.setOnClickListener(v -> showFragment(new IDCardFragment()));
//        btnAdd.setOnClickListener(v -> showFragment(new AddPersonFragment()));
        btnCamera.setOnClickListener(v -> {
            Log.d(TAG, "Camera button clicked");
            checkPermissionAndOpenCamera();
        });
    }



    private void checkPermissionAndOpenCamera(){
        if(ContextCompat.checkSelfPermission(this,CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED){
            showFragment(new CameraPreview());
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
                showFragment(new CameraPreview());
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showFragment(Fragment fragment){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment)
                .commit();
    }
}