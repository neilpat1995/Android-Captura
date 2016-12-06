package finalproject.mobileappclass.com.captura;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Toast;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;


import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static finalproject.mobileappclass.com.captura.R.id.takePhotoButton;
import static finalproject.mobileappclass.com.captura.R.id.te;


import finalproject.mobileappclass.com.captura.DatabaseHelper.CapturaDatabaseHelper;
import finalproject.mobileappclass.com.captura.ImageHandling.CloudVisionWrapper;

import finalproject.mobileappclass.com.captura.ImageHandling.ExifUtil;
import finalproject.mobileappclass.com.captura.ImageHandling.RealPathUtil;
import finalproject.mobileappclass.com.captura.Models.QuizScore;
import finalproject.mobileappclass.com.captura.Models.TranslationRequest;
import finalproject.mobileappclass.com.captura.SharedPreferencesHelper.PrefSingleton;
import finalproject.mobileappclass.com.captura.Translation.GoogleTranslateWrapper;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    PrefSingleton prefSingleton;
    private boolean permissionsGranted = false;
    private boolean hasTakenOrSelectedPhoto = false;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int IMG_CAPTURE_REQUEST_CODE = 200;
    private static final int IMG_UPLOAD_REQUEST_CODE = 300;
    private ImageView imageView;
    private TextToSpeech textToSpeech;
    private String translatedWord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermissions();

        prefSingleton = PrefSingleton.getInstance();
        prefSingleton.init(getApplicationContext());

        if(PrefSingleton.getInstance().readPreference("language") == null){
            PrefSingleton.getInstance().writePreference("language", "en");
        }
        textToSpeech = new TextToSpeech(getApplicationContext(), this);
        Button takePhotoButton = (Button) findViewById(R.id.takePhotoButton);
        imageView = (ImageView) findViewById(R.id.imageholder);
        Button uploadPhotoButton = (Button) findViewById(R.id.choosePhotoButton);
        Button recognizeImageButton = (Button) findViewById(R.id.recognize_image_button);

        //If user wants to take an image from the camera
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (permissionsGranted) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, IMG_CAPTURE_REQUEST_CODE);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Need Permission To Use Camera", Toast.LENGTH_LONG).show();
                }
            }
        });

        //If user wants to upload an existing image
        uploadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMG_UPLOAD_REQUEST_CODE);
            }
        });

        recognizeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasTakenOrSelectedPhoto)
                {
                    new ImageRecognitionTask().execute(((BitmapDrawable)imageView.getDrawable()).getBitmap());
                }
                else {
                    Toast.makeText(MainActivity.this, "Photo not taken or selected yet!", Toast.LENGTH_LONG).show();
                }
            }
        });

        //translation testing
        final EditText inputText = (EditText) findViewById(R.id.input_field) ;
        Button translateButton = (Button) findViewById(R.id.translate_button);
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                new GoogleTranslateTask().execute(inputText.getText().toString(), "en", PrefSingleton.getInstance().readPreference("language"));
            }
        });

        Button historyButton = (Button) findViewById(R.id.history_button);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, History.class);
                startActivity(intent);
            }
        });

        //test database stuff
        Button databaseButton = (Button) findViewById(R.id.db_button);
        databaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CapturaDatabaseHelper capturaDatabaseHelper = CapturaDatabaseHelper.getInstance(getApplicationContext());
                TranslationRequest translationRequest = new TranslationRequest("Hello", "Bonjour", "fr");
                TranslationRequest translationRequest1 = new TranslationRequest("Hello", "Hola", "esp");

                QuizScore quizScore = new QuizScore(10, "Test timestamp", "fr");
                QuizScore quizScore1 = new QuizScore(10, "Test timestamp2", "esp");

                //insert
                capturaDatabaseHelper.insertTranslationRequest(translationRequest);
                capturaDatabaseHelper.insertTranslationRequest(translationRequest);
                capturaDatabaseHelper.insertTranslationRequest(translationRequest1);
                capturaDatabaseHelper.insertTranslationRequest(translationRequest1);
                capturaDatabaseHelper.insertQuizScore(quizScore);
                capturaDatabaseHelper.insertQuizScore(quizScore1);

                //query
                ArrayList<TranslationRequest> requestList = capturaDatabaseHelper.getEntireHistory();
                ArrayList<QuizScore> quizScoreArrayList = capturaDatabaseHelper.getAllScores();

                for(TranslationRequest tr : requestList)
                {
                    Log.v("AndroidCaptura", tr.getInputWord() + " " + tr.getTranslatedWord() + " " + tr.getLanguageOfInterest());
                }

                for(QuizScore qs : quizScoreArrayList)
                {
                    Log.v("AndroidCaptura", ""+qs.getQuizScore() + " " + qs.getTimeStamp() + " " + qs.getLanguageOfInterest());
                }

                ArrayList<TranslationRequest> requestArrayList = capturaDatabaseHelper.findTranslationRequestsByLanguage("fr");
                ArrayList<QuizScore> quizScores = capturaDatabaseHelper.findQuizScoresByLanguage("esp");

                for(TranslationRequest t : requestArrayList)
                {
                    Log.v("AndroidCaptura", t.getInputWord() + " " + t.getTranslatedWord() + " " + t.getLanguageOfInterest());
                }

                for(QuizScore q : quizScores)
                {
                    Log.v("AndroidCaptura", ""+q.getQuizScore() + " " + q.getLanguageOfInterest() + " " + q.getTimeStamp());
                }

            }
        });



        Button ttsButton = (Button) findViewById(R.id.tts_button);
        ttsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(translatedWord != null)
                {
                    textToSpeech.speak(translatedWord, TextToSpeech.QUEUE_FLUSH, null, null);
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "You have not translated a word", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_settings:

                View settingsItem = findViewById(R.id.action_settings);
                Toast.makeText(MainActivity.this, "Select a language to learn", Toast.LENGTH_SHORT).show();

                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(MainActivity.this, settingsItem);

                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.popupmenu, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    public boolean onMenuItemClick(MenuItem item) {

                        PrefSingleton.getInstance().writePreference("language", (String) item.getTitleCondensed());
                        textToSpeech.setLanguage(new Locale((String) item.getTitleCondensed()));
                        setTextToSpeechLanguage();
                        Toast.makeText(MainActivity.this, "You are now learning " + item.getTitle(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
                popup.show();

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void checkAndRequestPermissions() {
        int cameraResult = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        int readExtResult = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeExtResult = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if ((cameraResult != PackageManager.PERMISSION_GRANTED) || (readExtResult != PackageManager.PERMISSION_GRANTED) || (writeExtResult != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
        } else {
            permissionsGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Captura Permissions are granted!", Toast.LENGTH_SHORT).show();
                permissionsGranted = true;
            } else {
                Toast.makeText(getApplicationContext(), "Need to enable Permissions!", Toast.LENGTH_LONG).show();
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    //TODO: EDIT THIS AS NEEDED TO HANDLE IMAGE CAPTURE
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMG_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            hasTakenOrSelectedPhoto = true;
        } else if (requestCode == IMG_UPLOAD_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            try
            {
                decodeFile(RealPathUtil.getRealPathFromURI_API19(getApplicationContext(), imageUri));
                hasTakenOrSelectedPhoto = true;
            } catch (Exception e) {
                Log.e("Captura", e.getMessage());
            }
        }
    }

    //TODO: May or may not have to create an asynctask for this if the operation causes the UI thread to crash
    public void decodeFile(String filePath) {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 1024;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp < REQUIRED_SIZE && height_tmp < REQUIRED_SIZE)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap b1 = BitmapFactory.decodeFile(filePath, o2);
        Bitmap b = ExifUtil.rotateBitmap(filePath, b1);
        imageView.setImageBitmap(b);

    }

    private class ImageRecognitionTask extends AsyncTask<Bitmap, Void, String>
    {
        @Override
        protected String doInBackground(Bitmap... bitmaps)
        {
            CloudVisionWrapper cloudVisionWrapper = new CloudVisionWrapper(bitmaps[0], getApplicationContext());
            return cloudVisionWrapper.performImageRecognition();
        }

        @Override
        protected void onPostExecute(String s) {
            Log.v("AndroidCaptura", s);
        }
    }

    private class  GoogleTranslateTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... strings)
        {
            GoogleTranslateWrapper googleTranslateWrapper = new GoogleTranslateWrapper(getApplicationContext());
            return googleTranslateWrapper.translate(strings[0], strings[1], strings[2]);
        }

        @Override
        protected void onPostExecute(String s)
        {
            translatedWord = s;
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onInit(int status)
    {
        if(status == TextToSpeech.SUCCESS)
        {
            setTextToSpeechLanguage();
        }
    }

    public void setTextToSpeechLanguage()
    {
        if(textToSpeech.isLanguageAvailable(new Locale(prefSingleton.readPreference("language"))) == TextToSpeech.LANG_AVAILABLE)
        {
            Toast.makeText(getApplicationContext(), "This language has TTS support", Toast.LENGTH_LONG).show();
            textToSpeech.setLanguage(new Locale(prefSingleton.readPreference("language")));
        }
        else
        {
            Toast.makeText(getApplicationContext(), "This language does not have TTS support", Toast.LENGTH_LONG).show();
            textToSpeech.setLanguage(Locale.US);
        }
    }

}