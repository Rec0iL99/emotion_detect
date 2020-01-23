package com.semicolon.emotiondetect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.*;
import java.lang.Object.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;


import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;


public class VideoActivity extends AppCompatActivity {

    private static final String TAG ="VideoActivity";
    private VideoView videoView;
    String videoString;
    String videoReaction;
    Uri videoUri;

    //camera2 api
    private TextureView textureView;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0,270);
        ORIENTATIONS.append(Surface.ROTATION_90,180);
        ORIENTATIONS.append(Surface.ROTATION_180,0);
        ORIENTATIONS.append(Surface.ROTATION_270,90);
    }

    private String cameraID;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    //save to file
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private List<String> finalEmotionList;


    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice=null;
        }
    };

    //testing feature
    // Add your Face endpoint to your environment variables.
    private  String apiEndpoint = System.getenv("FACE_ENDPOINT");
    // Add your Face subscription key to your environment variables.
    private  String subscriptionKey = System.getenv("FACE_SUBSCRIPTION_KEY");


    private FaceServiceClient faceServiceClient = new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0", "dedf0c13dea244619a42977bf1790858");

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;

    Bitmap processBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Log.d(TAG, "onCreate: videoActivity");

        Date d = new Date();
        final CharSequence videoTime  = DateFormat.format("EEEE, MMMM d, yyyy ", d.getTime());


        videoView = findViewById(R.id.video_view);

        finalEmotionList = new ArrayList<>();

        final Intent intent = getIntent();
        videoString = intent.getStringExtra("path");

        final String videoName=videoString.substring(videoString.lastIndexOf("/")+1);

        videoUri = Uri.parse(videoString);

        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(videoUri);
        mediaController.setAnchorView(videoView);
        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

                if(!finalEmotionList.isEmpty()) {
                    videoReaction = mostCommon(finalEmotionList);
                }

                Intent intentMain = new Intent(VideoActivity.this, MainActivity.class);
                intentMain.putExtra("videoName", videoName);
                intentMain.putExtra("videoTime", videoTime);
                intentMain.putExtra("videoThumb", videoString);
                intentMain.putExtra("videoReaction", videoReaction);
                startActivity(intentMain);

            }
        });

        //camera api
        textureView = findViewById(R.id.camera_view);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                takePicture();
                handler.postDelayed(this, 5000);
            }
        }, 5000);

    }

    private void takePicture() {
        if(cameraDevice == null)
            return;
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            //Capture image with custom size
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));


            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //Check orientation base on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

            file = new File(Environment.getExternalStorageDirectory()+"/"+ UUID.randomUUID().toString()+".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try{
                        image = reader.acquireLatestImage();
//                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                        byte[] bytes = new byte[buffer.capacity()];
//                        buffer.get(bytes);

//                        final Uri uri = Uri.fromFile(file);
//                        final Bitmap bitmap = MediaStore.Images.Media.getBitmap(
//                                getContentResolver(), uri);
//
//                        //detectAndFrame(bitmap);
//


                        //save(bytes);

                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        buffer.rewind();
                        byte[] data = new byte[buffer.capacity()];
                        buffer.get(data);
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                        detectAndFrame(bitmap);

                        save(data);

                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally {
                        {
                            if(image != null)
                                image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream outputStream = null;
                    try{
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                    }finally {
                        if(outputStream != null)
                            outputStream.close();
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast.makeText(VideoActivity.this, "Saved "+file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert  texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(VideoActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void updatePreview() {
        if(cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraID = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraID,stateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread= null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    // Detect faces by uploading a face image.
// Frame faces after detection.
    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    new FaceServiceClient.FaceAttributeType[] {
                                            FaceServiceClient.FaceAttributeType.Emotion }        // returnFaceAttributes:
                                /* new FaceServiceClient.FaceAttributeType[] {
                                    FaceServiceClient.FaceAttributeType.Age,
                                    FaceServiceClient.FaceAttributeType.Gender }
                                */
                            );
                            if (result == null){
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
//                        detectionProgressDialog = new ProgressDialog(VideoActivity.this);
//                        detectionProgressDialog.setMessage("Thanks for your Sign up...");
//                        detectionProgressDialog.setIndeterminate(false);
//                        detectionProgressDialog.setCancelable(true);
//                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
//                        runOnUiThread(new Runnable() {
//
//                            @Override
//                            public void run() {
//                                detectionProgressDialog.setMessage(progress[0]);
//                                // Stuff that updates the UI
//
//                            }
//                        });
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                        //detectionProgressDialog.dismiss();

                        if(!exceptionMessage.equals("")){
                            showError(exceptionMessage);
                        }
                        if (result == null) return;

                        imageBitmap.recycle();

                        String emotionR = getEmotion(result);

                        //Toast.makeText(getApplicationContext(), emotionR , Toast.LENGTH_LONG).show();
                        finalEmotionList.add(emotionR);

                    }
                };

        detectTask.execute(inputStream);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }})
                .create().show();
    }

    private String sendEmotion(Face[] faces) {

        String ff = null;
        if(faces!=null) {
            for (Face face : faces) {
                ff += face.faceAttributes.emotion.happiness;
            }
        }
        return ff;
    }

    private String getEmotion(Face[] faces) {
        List<Double> emotions = new ArrayList<>();

        if(faces!=null) {
            for(Face face : faces) {
                emotions.add(face.faceAttributes.emotion.surprise);
                emotions.add(face.faceAttributes.emotion.happiness);
                emotions.add(face.faceAttributes.emotion.anger);
                emotions.add(face.faceAttributes.emotion.contempt);
                emotions.add(face.faceAttributes.emotion.fear);
                emotions.add(face.faceAttributes.emotion.neutral);
                emotions.add(face.faceAttributes.emotion.sadness);
            }
        }

        Collections.sort(emotions);

        double max = emotions.get(emotions.size() - 1);

        String finalEmotion = "";

        if(faces!=null) {
            for(Face face : faces) {
                if(max == face.faceAttributes.emotion.anger) {
                    finalEmotion = "anger";
                } else if(max == face.faceAttributes.emotion.happiness) {
                    finalEmotion = "happy";
                } else if(max == face.faceAttributes.emotion.fear) {
                    finalEmotion = "fear";
                } else if(max == face.faceAttributes.emotion.contempt) {
                    finalEmotion = "contempt";
                } else if(max == face.faceAttributes.emotion.neutral) {
                    finalEmotion = "neutral";
                } else if(max == face.faceAttributes.emotion.sadness) {
                    finalEmotion = "sad";
                } else if(max == face.faceAttributes.emotion.surprise) {
                    finalEmotion = "surprise";
                }
            }
        }

        //Toast.makeText(getApplicationContext(), finalEmotion, Toast.LENGTH_LONG).show();

        return  finalEmotion;

    }

    public static <T> T mostCommon(List<T> list) {
        Map<T, Integer> map = new HashMap<>();

        for (T t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }

        Map.Entry<T, Integer> max = null;

        for (Map.Entry<T, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }

        return max.getKey();
    }


}
