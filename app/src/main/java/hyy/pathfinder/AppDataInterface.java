package hyy.pathfinder;

import android.location.Location;
import android.os.Bundle;

/**
 * Created by Prometheus on 28-Oct-16.
 */

public interface AppDataInterface
{
    void connected(Bundle bundle);
    void suspended(int errorCode);
    void locationChanged(Location location);
}
