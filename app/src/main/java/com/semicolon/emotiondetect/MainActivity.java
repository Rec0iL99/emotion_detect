package com.semicolon.emotiondetect;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    ListView reactionList;
    FloatingActionButton actionButton;

    private final int CODE_ONE_VIDEO = 1;

    Uri videoUri;

    //videoInfo
    String videoName;
    String videoTime;
    String videoThumb;
    String videoEmotionT;
    String videoEmotionF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: started");
        
        reactionList = findViewById(R.id.emotions_list);
        actionButton = findViewById(R.id.floating_button);

        final Intent intent = getIntent();
        videoName = intent.getStringExtra("videoName");
        videoTime = intent.getStringExtra("videoTime");
        videoThumb = intent.getStringExtra("videoThumb");
        videoEmotionT = intent.getStringExtra("videoReaction");

        if(videoEmotionT!=null) {
            if(videoEmotionT.equals("happy")) {
                videoEmotionF = "😂";
            } else if(videoEmotionT.equals("angry")) {
                videoEmotionF = "😠";
            } else if(videoEmotionT.equals("sad")) {
                videoEmotionF = "😞";
            } else if(videoEmotionT.equals("surprise")) {
                videoEmotionF = "😮";
            } else if(videoEmotionT.equals("neutral")) {
                videoEmotionF = "😐";
            } else if(videoEmotionT.equals("fear")) {
                videoEmotionF = "😱";
            } else if(videoEmotionT.equals("contempt")) {
                videoEmotionF = "🙄";
            } else {
                videoEmotionF = "No reaction";
            }

        }

        Reaction r1 = new Reaction(videoName, videoTime, videoEmotionF, videoThumb);

        ArrayList<Reaction> reactionArrayList = new ArrayList<Reaction>();
        reactionArrayList.add(r1);

        ReactionListAdapter adapter = new ReactionListAdapter(this, R.layout.emotion_layout, reactionArrayList);
        reactionList.setAdapter(adapter);

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Video"),CODE_ONE_VIDEO);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==CODE_ONE_VIDEO && resultCode==RESULT_OK) {
            videoUri = data.getData();
            try{
                Intent intent = new Intent(MainActivity.this,
                            VideoActivity.class);
                intent.putExtra("path", videoUri.toString());
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



}
