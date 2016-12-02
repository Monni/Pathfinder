package hyy.pathfinder;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import hyy.pathfinder.Activities.MainActivity;

public class Splash extends AppCompatActivity {

    /** Splash Screen duration in milliseconds **/
    private final int SPLASH_DISPLAY_LENGTH = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // New handler to start MainActivity and close this Splash-screen after desired time
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Create an Intent that will start MainActivity
                Intent mainActivity = new Intent(Splash.this, MainActivity.class);
                Splash.this.startActivity(mainActivity);
                Splash.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
