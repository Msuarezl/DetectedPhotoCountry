package com.example.evaluacion;

import android.content.Intent;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evaluacion.WebServices.Asynchtask;
import com.example.evaluacion.WebServices.WebService;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.LocationInfo;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements Asynchtask {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs";
    private static final int REQUEST_GALLERY = 1;
    private Bitmap bitmap;
    private ImageView imageView;
    private TextView textView;
    private Feature feature;
    private JSONObject jsonObject;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        starUp();

    }


    public void openGallery(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK).setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri imgUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
            } catch (IOException e) {
                Log.e("ERROR", "Ha salido un error " + e.getMessage());
            }
            imageView.setImageBitmap(bitmap);
            detectLandmarks(bitmap, feature);

        } else {
        }
    }


    private void detectLandmarks(Bitmap bitmap, Feature feature) {
        List<Feature> featureList = new ArrayList<>();
        featureList.add(feature);
        List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setFeatures(featureList);
        annotateImageRequest.setImage(getImageEncode(bitmap));
        annotateImageRequests.add(annotateImageRequest);

        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... objects) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);
                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);
                    Vision vision = builder.build();
                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(annotateImageRequests);
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);
                } catch (IOException e) {
                    Log.e("ERROR", "Error de Api: " + e.getMessage());
                }
                return "";
            }

            protected  void onPostExecute(String result) {
                String countryCode = "";
                try {
                    if (result != "") {
                        countryCode = CountryCode(result);
                        textView.setText(countryCode);
                    } else {
                        textView.setText("No hay información");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private String formatAnnotation(List<EntityAnnotation> entityAnnotation) {
        String msg = "";
        if (entityAnnotation != null) {
            for (EntityAnnotation entity : entityAnnotation) {
                LocationInfo info = entity.getLocations().listIterator().next();
                msg = info.getLatLng().getLatitude() + " " +  info.getLatLng().getLongitude();
                msg += ":";
            }
        } else {
            msg = "";
        }
        return msg;
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        AnnotateImageResponse imageResponses = response.getResponses().get(0);
        List<EntityAnnotation> entityAnnotations;
        String msg = "";
        entityAnnotations = imageResponses.getLandmarkAnnotations();
        msg = formatAnnotation(entityAnnotations);
        return msg;
    }

    private Image getImageEncode(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    private String CountryCode(String points) throws JSONException {
        String[] vectorPoints = points.split(":");
        for (String point : vectorPoints) {
            String[] latlng = point.split(" ");
            Iterator<String> temp = jsonObject.keys();
            while (temp.hasNext()) {
                String key = temp.next();
                JSONObject country = jsonObject.getJSONObject(key);
                JSONObject rectangle = country.getJSONObject("GeoRectangle");
                if (Double.valueOf(latlng[0]) <= rectangle.getDouble("North") &&
                        Double.valueOf(latlng[0]) >= rectangle.getDouble("South") &&
                        Double.valueOf(latlng[1]) <= rectangle.getDouble("East") &&
                        Double.valueOf(latlng[1]) >= rectangle.getDouble("West")) {
                    return country.getJSONObject("CountryCodes").getString("iso2");
                }
            }
        }
        return "";
    }


    public void buscra(View view) {
        if(textView.getText().equals(""))
        startActivity(new Intent(MainActivity.this,MapsActivity.class).putExtra("countryCode",textView.getText()));
        else
            Toast.makeText(this, "Carge la Imagen o Espere que detecte el pais", Toast.LENGTH_LONG).show();


    }
    private void starUp()
    {
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);

        feature = new Feature();
        feature.setType("LANDMARK_DETECTION");
        feature.setMaxResults(10);
        bitmap = null;
        Map<String, String> datos = new HashMap<String, String>();
        WebService ws= new WebService("http://www.geognos.com/api/en/countries/info/all.json", datos, this, this);
        ws.execute("");
    }
    @Override
    public void processFinish(String result) throws JSONException {
        jsonObject =  new JSONObject(result);
        jsonObject = jsonObject.getJSONObject("Results");
  //      Log.i("processFinish", jsonObject.toString());
    //    listaPaises=Paises.JsonObjectsBuild(jsonObject);
    }
}