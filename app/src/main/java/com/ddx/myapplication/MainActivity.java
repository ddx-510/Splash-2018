package com.ddx.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int requestPermissionID = 101;
    SurfaceView mCameraView;
    TextView mTextView;
    CameraSource mCameraSource;
    private Button button;
    private TextView textView;
    private TextView textView2;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
    private Intent recognizerIntent;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private AIDataService aiDataService;
    private String tempstring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textview);
        textView2 = findViewById(R.id.textview2);

        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);

        //initialise the camera source, idk why it will not work under onclick
        startCameraSource();

        // usual way of using button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startASr();
            }
        });

        setupButton();
        setupAsr();
        setupTTS();
        setupNLU();
    }


    private void setupButton() {
        String BUTTON_ACTION = "com.gowild.action.clickDown.action";

        //intentfiler
        intentFilter = new IntentFilter();
        intentFilter.addAction(BUTTON_ACTION);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startASr();
            }
        };

        registerReceiver(broadcastReceiver, intentFilter);
    }


    private void setupAsr() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.EXTRA_LANGUAGE_MODEL);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                Log.e("asr", "Error:" + Integer.toString(error));

            }

            @Override
            // give back the results processed
            public void onResults(Bundle results) {
                // generate a list based on the words heard
                List<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                String text = null;

                if (texts == null || texts.isEmpty()) {
                    textView.setText("Please Try Again");
                } else {
                    // here we simply use the first one , we dun know whether this one is the best one
                    text = texts.get(0);
                }
                textView.setText(text);

                // simple case of NLU
                /*String responseText;
                if (text.equalsIgnoreCase("hello")){
                    responseText = "hi there";
                }
                else{
                    responseText = "what are you saying?";
                }
                */

                startNLU(text);

            }

            @Override
            // haven't finished listened but alr hv some partial results
            // prediction
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

    }

    private void startASr() { // when to start listening
        speechRecognizer.startListening(recognizerIntent);
    }

    private void setupTTS() {// setting up test to speech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                textToSpeech.setLanguage(Locale.ENGLISH);
                // f tp indicate it is a float
                textToSpeech.setSpeechRate(1.0f);
            }
        });
    }

    private void startTTS(final String text) {
        // queue_flush do not wait until, just start a new text immediately
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView2.setText(text);
            }
        });


    }

    private void setupNLU() {
        String clientAccessToken = "6159bd9364d84847ae512fb9b340e30b";
        // set up ai configuration
        AIConfiguration aiConfiguration = new AIConfiguration(clientAccessToken,
                AIConfiguration.SupportedLanguages.English);

        // set up ai service
        aiDataService = new AIDataService(aiConfiguration);
    }

    private void startNLU(String text) {
        final AIRequest aiRequest = new AIRequest();
        aiRequest.setQuery(text);

        // put everything as a runnable so that it can be run on thread
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //send the request over
                try {
                    // using ai service to get ai response from server
                    AIResponse aiResponse = aiDataService.request(aiRequest);
                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String speech = fulfillment.getSpeech();

                    startTTS(speech);
                    if (Objects.equals(speech.substring(0, 4), "Okay")) {
                        startCameraSource();
                    }
                    // IF THE RESPONSE IS OKAY, START OUR OCR SCANNING THE ARTICLE AND READ
                    if (Objects.equals(speech.substring(0, 5), "Thank")) {
                        mCameraSource.stop();
                        startTTS(tempstring);
                    }
                } catch (AIServiceException e) {
                    Log.e("nlu", e.getMessage(), e);
                }


            }
        };

        // run the ai processing on the thread
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != requestPermissionID) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mCameraSource.start(mCameraView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startCameraSource() {
        Log.e("hi", "hi");
        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        } else {

            //Initialize camera source to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });
            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {

                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); i++) {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                mTextView.setText(stringBuilder.toString());
                                tempstring = stringBuilder.toString();
                            }
                        });
                    }
                }
            });
        }
    }
}
