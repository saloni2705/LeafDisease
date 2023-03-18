package com.example.leafdisease;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.renderscript.Element;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.leafdisease.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    TextView result, demoTxt, classified,clickHere;
    ImageView imageView, arrowImage;
    Button picture;

    int imageSize = 224; // default image size

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);

        demoTxt = findViewById(R.id.demoText);
        clickHere = findViewById(R.id.click_here);
        arrowImage = findViewById(R.id.demoArrow);
        classified = findViewById(R.id.classified);

        demoTxt.setVisibility(View.VISIBLE);
        clickHere.setVisibility(View.GONE);
        arrowImage.setVisibility(View.VISIBLE);
        classified.setVisibility(View.GONE);
        result.setVisibility(View.GONE);

        picture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                //launch camera if we have permission 5.26 of video
                if(checkSelfPermission(Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED){
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent,1);
                }else {
                    //request camera permision if we dont have
                    requestPermissions(new String[]{Manifest.permission.CAMERA},100);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==1 && resultCode== RESULT_OK){
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(),image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image,dimension,dimension);
            imageView.setImageBitmap(image);

            demoTxt.setVisibility(View.GONE);
            clickHere.setVisibility(View.VISIBLE);
            arrowImage.setVisibility(View.GONE);
            classified.setVisibility(View.VISIBLE);
            result.setVisibility(View.VISIBLE);

            image = Bitmap.createScaledBitmap(image,imageSize,imageSize,false);
            classifyImage(image);


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void classifyImage(Bitmap image) { //9:27 of video
        try {
            DiseaseDetection model = DiseaseDetection.newInstance(getApplicationContext());

            //create input for reference 10:11 of video
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1,224,224,3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            //get 1D array of 224 * 244 pixels
            int [] intValue = new int[imageSize * imageSize];
            image.getPixels(intValue, 0,image.getWidth(),0,0,image.getWidth(),image.getHeight());

            // iterate over pixels and extract r g b values
            int pixel =0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValue[pixel++];
                    byteBuffer.putFloat(((val>>16) & 0xFF)*(1.f/255.f));
                    byteBuffer.putFloat(((val>>8) & 0xFF)*(1.f/255.f));
                    byteBuffer.putFloat((val & 0xFF)*(1.f/255.f));
                    
                }
                
            }

            inputFeature0.loadBuffer(byteBuffer);

            DiseaseDetection.outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0SsTensorBuffer();

            float[]confidence = outputFeature0.getFloatArray();

            int maxPos = 0;
            float maxconfidence = 0;
            for (int i = 0; i < confidence.length; i++) {
                if (confidence[i]>maxconfidence){
                    maxconfidence=confidence[i];
                    maxPos=i;
                }
                
            }
            String[] classes = {"","","","","",""};
            result.setText(classes[maxPos]);
            result.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

        }catch (IOException e){
            // to handle the exception
        }

    }
}