package hyy.pathfinder;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private JSONArray stations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FetchDataTask task = new FetchDataTask();
        //task.execute("http://ptm.fi/android/highscore.json");
        task.execute("http://rata.digitraffic.fi/api/v1/metadata/stations.json");
    }

    class FetchDataTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            JSONObject json = null;
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
                json = new JSONObject(stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return json;
        }

        protected void onPostExecute(JSONObject json) {
            StringBuffer text = new StringBuffer("");
            try {
    //            stations = json.getJSONArray("highscores");
               JSONArray test = new JSONArray(json);

                //
                //for (int i=0;i < test.length();i++) {
                  //  JSONObject hs = test.getJSONObject(i);
                   // text.append(hs.getString("stationName")+ "\n");
                //}

               // for (int i=0;i < stations.length();i++) {
                 //   JSONObject hs = stations.getJSONObject(i);
                   // text.append(hs.getString("stationName") +"\n");
                //}
            } catch (JSONException e) {
                Log.e("JSON", "Error getting data.");
            }

            TextView textView = (TextView) findViewById(R.id.textView);
            textView.setText(text);
        }
    }

}
