package com.example.weatherapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final String inputKey = BuildConfig.API_KEY;
    private String inputCity;
    private TextInputLayout layout;
    private TextView location;
    private TextView temperature;
    private TextView weather;
    private TextView high;
    private TextView low;
    private TextView date;
    private EditText editText;
    private ImageView icon;
    private RequestQueue requestQueue;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = (TextInputLayout) findViewById(R.id.layout);
        location = (TextView) findViewById(R.id.location);
        temperature = (TextView) findViewById(R.id.temperature);
        weather = (TextView) findViewById(R.id.weather);
        high = (TextView) findViewById(R.id.high);
        low = (TextView) findViewById(R.id.low);
        date = (TextView) findViewById(R.id.date);
        editText = (EditText) findViewById(R.id.editText);
        icon = (ImageView) findViewById(R.id.icon);
        requestQueue = Volley.newRequestQueue(this);
        inputCity = "Ottawa";
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestPermission();
        getCurrentLocation();
        getWeather();
    }

    private void getWeather() {
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == editText.getId())
                    editText.setCursorVisible(true);
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean handled = false;
                editText.setCursorVisible(false);
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    inputCity = editText.getText().toString().trim();
                    parseData(getURL());
                    clearTextField();
                    handled = true;
                }
                return handled;
            }
        });
    }

    private String getURL() {
        return "https://api.openweathermap.org/data/2.5/weather?q=" + inputCity + "&appid=" + inputKey + "&units=metric";
    }

    private void getDateAndTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss z", Locale.getDefault());
        String currentDate = "Last Updated: " + sdf.format(new Date());
        date.setText(currentDate);
    }

    private void getAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Try Again!");
        builder.setMessage("There was an error with the city name.");
        builder.setNegativeButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
            dialog.cancel();
        });
        builder.setCancelable(true);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void parseData(String URL) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String nameAPI = response.getString("name");
                    JSONObject mainAPI = response.getJSONObject("main");
                    JSONObject weatherAPI = response.getJSONArray("weather").getJSONObject(0);
                    JSONObject sysAPI = response.getJSONObject("sys");
                    String locationText = nameAPI + ", " + sysAPI.getString("country");
                    String temperatureText = mainAPI.getInt("temp") + "°C";
                    String weatherText = weatherAPI.getString("main");
                    String highText = "H:" + mainAPI.getInt("temp_max") + "°C";
                    String lowText = "L:" + mainAPI.getInt("temp_min") + "°C";
                    String iconURL = "https://openweathermap.org/img/wn/" + weatherAPI.getString("icon") + "@4x.png";
                    location.setText(locationText);
                    temperature.setText(temperatureText);
                    weather.setText(weatherText);
                    high.setText(highText);
                    low.setText(lowText);
                    Picasso.get().load(iconURL).resize(300,0).into(icon);
                    getDateAndTime();
                } catch (JSONException e) {
                    getAlertDialog();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                getAlertDialog();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void clearTextField() {
        editText.setText("");
        InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(editText.getApplicationWindowToken(), 0);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, 101);
    }
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            parseData(getURL());
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String URL = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude +"&appid=" + inputKey;
                    parseData(URL);
                }
            });
        }
    }
}