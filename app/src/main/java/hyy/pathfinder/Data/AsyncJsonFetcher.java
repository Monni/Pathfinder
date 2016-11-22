package hyy.pathfinder.Data;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import hyy.pathfinder.Interfaces.AsyncResponse;

/**
 * Created by Kotimonni on 23.10.2016.
 */

public class AsyncJsonFetcher extends AsyncTask<String, Void, JSONArray> {
    private int mode;
    private boolean jsonException = false;
    public AsyncResponse delegate = null;
    public AsyncJsonFetcher(AsyncResponse Delegate)
    {
        delegate = Delegate;
    }
        @Override
        protected JSONArray doInBackground(String... urls) {
            Log.d("AsyncJsonFetcher", "Started");
            HttpURLConnection urlConnection = null;
            JSONArray json = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();   // Open connection to rata.digitraffic.fi
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                json = new JSONArray(stringBuilder.toString());
            } catch (IOException e) {
                Log.d("AsyncJsonFetcher", "doInBackground:" + e.getMessage());
                e.printStackTrace();
            } catch (JSONException e) {
                Log.d("AsyncJsonFetcher", "doInBackground:" + e.getMessage());
                jsonException = true;
                e.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return json;
        }

        protected void onPostExecute(JSONArray json) {
            Log.d("asyncJsonFetcher", "onPostExecute, mode "+mode);
            if (json == null)
            {
                Log.d("asyncJsonFetcher", "JSONArray null");
            }
            delegate.onAsyncJsonFetcherComplete(mode, json, jsonException); // calls AsyncResponse.java interface
        }

    public void fetchStations(String url) {
        mode = 1;
       // AsyncJsonFetcher asyncJsonFetcher = new AsyncJsonFetcher(delegate);
        this.execute(url);
    }
    public void fetchTrains(String url) {
        Log.d("fetchTrains","started");
        mode = 2;
        try {
            this.execute(url);
        } catch (Exception e) {
            Log.d("AsyncJsonFetcher", "fetchTrains:" + e.getMessage());
            e.printStackTrace();
        }

    }
}
