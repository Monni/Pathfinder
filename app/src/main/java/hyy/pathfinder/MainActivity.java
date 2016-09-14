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

import org.json.JSONArray;
import org.json.JSONObject;
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
    JSONObject json;

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

            //TextView tv = (TextView) findViewById(R.id.txtJSON);
            //tv.setText(response);
            try
            {
                json = new JSONObject(response);
                TextView tvStartLatitude = (TextView) findViewById(R.id.txtStartLat);
                TextView tvStartLongitude = (TextView) findViewById(R.id.txtStartLong);
                TextView tvEndLatitude = (TextView) findViewById(R.id.txtEndLat);
                TextView tvEndLongitude = (TextView) findViewById(R.id.txtEndLong);

                JSONArray routesArray = json.getJSONArray("routes");
                JSONObject routes = routesArray.getJSONObject(0);
                JSONArray legsArray = routes.getJSONArray("legs");
                JSONObject legs = legsArray.getJSONObject(0);

                String endLocation = legs.getString("end_address");
                JSONObject endLoc = legs.getJSONObject("end_location");
                String endLatitude = endLoc.getString("lat");
                String endLongitude = endLoc.getString("lng");

                String startLocation = legs.getString("start_address");
                JSONObject startLoc = legs.getJSONObject("start_location");
                String startLatitude = startLoc.getString("lat");
                String startLongitude = startLoc.getString("lng");

                tvStartLatitude.setText(startLatitude);
                tvStartLongitude.setText(startLongitude);
                tvEndLatitude.setText(endLatitude);
                tvEndLongitude.setText(endLongitude);

            }
            catch (Exception e)
            {
                TextView footer = (TextView) findViewById(R.id.txtFooter);
                footer.setText(e.getMessage());
            }

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
            String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + from + "&destination=" + to + "&key=" + getResources().getString(R.string.google_maps_key);

            FetchDataTask task = new FetchDataTask();
            task.execute(urlString);


        }
        catch (Exception ex)
        {
            TextView footer = (TextView) findViewById(R.id.txtFooter);
            footer.setText(ex.getMessage());
        }

    }
}
