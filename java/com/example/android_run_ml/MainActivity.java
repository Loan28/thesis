package com.example.android_run_ml;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import android.content.res.AssetFileDescriptor;

import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String MODEL_PATH = "mobilenet_v1_1.0_224.tflite";
    private static final String LABEL_PATH = "labels_mobilenet_quant_v1_224.txt";

    private Classifier test_Classifier;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button= (Button)findViewById(R.id.button);
        TextView t = (TextView)findViewById(R.id.display_1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AssetManager am = getResources().getAssets();

                    InputStream is=null;
                try {
                    is = am.open("dog.jpeg");
                    Bitmap bMap = BitmapFactory.decodeStream(is);
                    // use the input stream as you want
                    TextView t = (TextView)findViewById(R.id.display_1);
                    try {
                        test_Classifier = Classifier.loadClassifier(getAssets(), MODEL_PATH);
                        test_Classifier.loadLabelList(getAssets(), LABEL_PATH);
                        t.setText(test_Classifier.recognizeImage(bMap));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }

}
