package com.github.sgdesmet.androidutils.location;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import android.util.Log;
import com.github.sgdesmet.androidutils.location.impl.LocationFinder;

/**
 * TODO description
 * <p/>
 * Date: 30/01/13
 * Time: 12:11
 *
 * @author: sgdesmet
 */
public class LocationFinderFacory {
    private static final String TAG = LocationFinderFacory.class.getSimpleName();

    public static ILocationFinder getNetworkLocationFinder(Context context){
        Log.d(TAG, "Getting network location finder");
        return new LocationFinder(context, LocationManager.NETWORK_PROVIDER);
    }

    public static ILocationFinder getGPSLocationFinder(Context context){
        Log.d(TAG, "Getting gps location finder");
        return new LocationFinder(context, LocationManager.GPS_PROVIDER);
    }

    public static ILocationFinder getFastLocationFinder(Context context){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setBearingRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return new LocationFinder(context, criteria);
    }

    public static ILocationFinder getAccurateLocationFinder(Context context){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        return new LocationFinder(context, criteria);
    }

}
