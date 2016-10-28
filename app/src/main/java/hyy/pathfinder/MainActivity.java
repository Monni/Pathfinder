package hyy.pathfinder;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;


import java.text.DecimalFormat;
import java.util.Calendar;



public class MainActivity extends AppCompatActivity implements AppDataInterface {
    // Create DecimalFormat to force date and time into two digit format
    private DecimalFormat doubleDigitFormat = new DecimalFormat("00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // HUOM SEURAAVA AJOJÄRJESTYS KRIITTINEN!
        // Tätä ApplicationData.asdf -möykkyä ei tarvitse ajaa uudelleen ohjelman ajon aikana.
        // Ainoastaan setApplicationCallbacksDelegate täytyy asettaa uudelleen kun siirrytään uuteen aktiviteettiin. Kyseisen aktiviteetin on implementoitava AppDataInterface.
        ApplicationData.mContext = getApplicationContext();
        ApplicationData.setApplicationDataCallbacks();
        // implementoitava AppDataInterface kutsuvaan luokkaan ennen kuin delegaatin asetus toimii
        ApplicationData.setApplicationDataCallbacksDelegate(this);
        ApplicationData.setLocationListener();
        ApplicationData.buildGoogleApiClient(this);
        ApplicationData.createLocationRequest();



        Switch gpsSwitch = (Switch) findViewById(R.id.gpsSwitch);
        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                Log.d("In onCheckedChanged", "start");
                if (isChecked == true && LocationPermissionAgent.isLocationEnabled(getBaseContext()) == false)
                {
                    Log.d("In onCheckedChanged", "first");
                    PermissionDialogFragment permissionDialogFragment = new PermissionDialogFragment();
                    permissionDialogFragment.show(getSupportFragmentManager(),"permissionRequest");
                }
                else if(isChecked == true && LocationPermissionAgent.isLocationEnabled(getBaseContext()) == true)
                {
                    Log.d("In onCheckedChanged", "second");
                    ApplicationData.getLastLocation(MainActivity.this);
                    EditText etOrigin = (EditText) findViewById(R.id.etOrigin);
                    etOrigin.setEnabled(false);
                    ApplicationData.startLocationUpdates(MainActivity.this);
                }
                else if(isChecked == false)
                {
                    Log.d("In onCheckedChanged", "third");
                    EditText etOrigin = (EditText) findViewById(R.id.etOrigin);
                    etOrigin.setEnabled(true);
                    //ApplicationData.stopLocationUpdates();
                }
            }
        });



        // Calendar for departure date and time, gets current system datetime
        final Calendar calendar = Calendar.getInstance();
        final int month = calendar.get(Calendar.MONTH) + 1;
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        final int year = calendar.get(Calendar.YEAR);
        final int hour = Integer.valueOf(doubleDigitFormat.format(calendar.get(Calendar.HOUR_OF_DAY)));
        final int minute = Integer.valueOf(doubleDigitFormat.format(calendar.get(Calendar.MINUTE)));


        // Create listener for "immediately" button. If checked, disable departure date
        CompoundButton locStartImmediately = (Switch) findViewById(R.id.locStartImmediately);
        locStartImmediately.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    // Disable date, set current date
                    findViewById(R.id.locStartDate).setEnabled(false);
                    EditText date = (EditText) findViewById(R.id.locStartDate);
                    date.setText(day +"."+ month +"."+ year);
                    // Disable time, set current time
                    findViewById(R.id.locStartTime).setEnabled(false);
                    EditText time = (EditText) findViewById(R.id.locStartTime);
                    time.setText(doubleDigitFormat.format(hour) +":"+ doubleDigitFormat.format(minute));
                }
                else  {
                    // Return control to date / EditText
                    findViewById(R.id.locStartDate).setEnabled(true);
                    EditText date = (EditText) findViewById(R.id.locStartDate);
                    date.setHint("dd.mm.yyyy");
                    // Return control to time / EditText
                    findViewById(R.id.locStartTime).setEnabled(true);
                    EditText time = (EditText) findViewById(R.id.locStartTime);
                    time.setHint("hh:mm");
                }
            }
        });

    }

    @Override
    public void suspended(int errorCode)
    {

    }

    @Override
    public void connected(Bundle bundle)
    {

    }

    @Override
    public void locationChanged(Location location)
    {

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


    public void btnRoute_clicked(View view)
    {
        // Convert departure date into suitable format (YYYY-MM-DD)
        EditText locStartDate = (EditText) findViewById(R.id.locStartDate);
        String[] startDateString = locStartDate.getText().toString().split("\\.");
        String locStartDateConverted = "";
        for (int i = startDateString.length; i > 0; i--) {
            locStartDateConverted += doubleDigitFormat.format(Double.valueOf(startDateString[i-1]));
            if (i-1 != 0) {
                locStartDateConverted += "-";
            }
        }
        String originDate = locStartDateConverted;
        // Get departure time (HH:MM)
        EditText StartTime = (EditText) findViewById(R.id.locStartTime);
        String originTime = StartTime.getText().toString();


        Intent intent = new Intent(this, RoutePresenter.class);
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
        else {
            origin = etOrigin.getText().toString();
        }
        String destination = etDestination.getText().toString();


        intent.putExtra("origin", origin);
        intent.putExtra("originDate", originDate);
        intent.putExtra("originTime", originTime);
        intent.putExtra("destination", destination);
        intent.putExtra("useMyLocation", useMyLocation);

        startActivity(intent);
    }
}

