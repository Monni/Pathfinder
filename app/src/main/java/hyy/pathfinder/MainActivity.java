package hyy.pathfinder;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class MainActivity extends AppCompatActivity implements AsyncResponse {

    @Override
    public void getSpaceTimeFinish(List<String> output){
        //Here you will receive the result fired from async class
        //of onPostExecute(result) method.
        TextView tv = (TextView) findViewById(R.id.txtJSON);
        StringBuilder result = new StringBuilder();
        for (String data: output)
        {
            result.append(data);
        }
        String asdf = result.toString();
        tv.setText(asdf);

    }

    @Override
    public void getRouteFinish(PolylineOptions polylineOptions){
        //Here you will receive the result fired from async class
        //of onPostExecute(result) method.

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch gpsSwitch = (Switch) findViewById(R.id.gpsSwitch);
        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked == true && LocationPermissionAgent.isLocationEnabled(getBaseContext()) == false)
                {
                    PermissionDialogFragment permissionDialogFragment = new PermissionDialogFragment();
                    permissionDialogFragment.show(getSupportFragmentManager(),"permissionRequest");
                }
                else if(isChecked == true && LocationPermissionAgent.isLocationEnabled(getBaseContext()) == true)
                {
                    EditText etOrigin = (EditText) findViewById(R.id.etOrigin);
                    etOrigin.setEnabled(false);
                }
                else if(isChecked == false)
                {
                    EditText etOrigin = (EditText) findViewById(R.id.etOrigin);
                    etOrigin.setEnabled(true);
                }

            }
        });

    }

    public void startMap(View view)
    {
        Intent intent = new Intent(this, MapsActivity.class);
        EditText etOrigin = (EditText) findViewById(R.id.etOrigin);
        EditText etDestination = (EditText) findViewById(R.id.etDestination);
        Switch gpsSwitch = (Switch) findViewById(R.id.gpsSwitch);
        String origin;
        boolean useMyLocation = false;

        if(gpsSwitch.isChecked())
        {
            origin = "my_location";
            useMyLocation = true;
        }
        else {origin = etOrigin.getText().toString();}

        String destination = etDestination.getText().toString();

        intent.putExtra("origin", origin);
        intent.putExtra("destination", destination);
        intent.putExtra("useMyLocation", useMyLocation);

        startActivity(intent);
    }

    public void testJSON(View view)
    {
        Router router = new Router();
        router.delegate = this;
        EditText etOrigin = (EditText) findViewById(R.id.etOrigin);
        EditText etDestination = (EditText) findViewById(R.id.etDestination);
        router.getTravelDistanceAndDuration(etOrigin.getText().toString(), etDestination.getText().toString(),getBaseContext());

    }

}

