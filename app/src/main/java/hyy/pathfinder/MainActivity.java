package hyy.pathfinder;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    final int ACTIVITY_MAPS = 700;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startMap(View view)
    {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }



    class FetchDataTask extends AsyncTask<String, Void, String> {

        protected  void onPreExecute() {

        }

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                return stringBuilder.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return null;
        }

        protected void onPostExecute(String response) {

        }
    }



    public void getJSON(View view)
    {
        try
        {
            EditText etFrom = (EditText) findViewById(R.id.etFrom);
            String from = etFrom.getText().toString();

            EditText etTo = (EditText) findViewById(R.id.etTo);
            String to = etTo.getText().toString();
            String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + from + "&destination=" + to + "&key=AIzaSyABeFMjmwEa00fR4DwKg9U8Ty4kkXB6XFk";
           // URL url = new URL(urlString);

            HttpHandler httphandler = new HttpHandler();
            String data = httphandler.makeServiceCall(urlString);

            TextView tv = (TextView) findViewById(R.id.txtJSON);
            tv.setText(data);


            /*FetchDataTask task = new FetchDataTask();
            task.execute(url);
*/
/*
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());


            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                result.append(line);
            }
*/
        }
        catch (Exception ex)
        {
            Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
        }

    }
}
