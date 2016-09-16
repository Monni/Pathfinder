package hyy.pathfinder;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;



public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void startMap(View view)
    {
        Intent intent = new Intent(this, MapsActivity.class);
        EditText etOrigin = (EditText) findViewById(R.id.etOrigin);
        EditText etDestination = (EditText) findViewById(R.id.etDestination);

        String origin = etOrigin.getText().toString();
        String destination = etDestination.getText().toString();

        intent.putExtra("origin", origin);
        intent.putExtra("destination", destination);

        startActivity(intent);
    }

}
