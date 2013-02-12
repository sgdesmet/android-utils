/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.sgdesmet.androidutils.location.impl;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.github.sgdesmet.androidutils.location.ILocationFinder;
import com.github.sgdesmet.androidutils.location.NoProviderAvailableException;

import java.util.List;

/**
 * Legacy implementation of Last Location Finder for all Android platforms
 * down to Android 1.6.
 * <p/>
 * This class let's you find the "best" (most accurate and timely) previously
 * detected location using whatever providers are available.
 * <p/>
 * Where a timely / accurate previous location is not detected it will
 * return the newest location (where one exists) and setup a one-off
 * location update to find the current location.
 */
public class LocationFinder implements ILocationFinder {

    private static final int MIN_TIME = 5 * 1000;
    private static final int MIN_DISTANCE = 1000;
    protected static String TAG = LocationFinder.class.getSimpleName();

    protected LocationListener locationListener;
    protected LocationManager locationManager;
    protected Criteria criteria;
    protected Context context;
    protected String provider;
    private Handler handler;

    /**
     * Construct a new Legacy Last Location Finder.
     *
     * @param context Context
     */
    public LocationFinder(Context context, Criteria criteria) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        this.criteria = criteria;
        provider = locationManager.getBestProvider(criteria, true);
        this.context = context;
    }

    public LocationFinder(Context context, String provider) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        this.provider = provider;
        this.context = context;
    }


    @Override
    public void getLocation(int minDistance, long minTime, LocationListener l) throws NoProviderAvailableException {
        getLocation(minDistance, minTime, -1, 0 ,l);
    }

    /**
     *
     * Request a location with the specified accuracy.
     * If available, returns the most accurate and timely previously detected location,
     * if it matches the specified maximum distance or latency.
     * Otherwise, a one-off location update is returned via the {@link android.location.LocationListener}
     *
     * @param minDistance Minimum distance before we require a location update.
     * @param minTime     Minimum time required between location updates.
     * @return The most accurate and / or timely previously detected location.
     */
    @Override
    public void getLocation(final int minDistance, final long minTime, final long maxFixTime, final int maxRetries, final LocationListener l) throws NoProviderAvailableException {

        locationListener = l;

        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MAX_VALUE;

        // Iterate through all the providers on the system, keeping
        // note of the most accurate result within the acceptable time limit.
        // If no result is found within maxTime, return the newest Location.
        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider : matchingProviders) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time < minTime && accuracy < bestAccuracy)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                } else if (time > minTime && bestAccuracy == Float.MAX_VALUE && time < bestTime) {
                    bestResult = location;
                    bestTime = time;
                }
            }
        }

        // If the best result is beyond the allowed time limit, or the accuracy of the
        // best result is wider than the acceptable maximum distance, request a single update.
        // This check simply implements the same conditions we set when requesting regular
        // location updates every [minTime] and [minDistance].
        // Prior to Gingerbread "one-shot" updates weren't available, so we need to implement
        // this manually.
        if (bestResult == null || bestTime > minTime || bestAccuracy > minDistance) {
            if (locationListener != null ){
                //get provider, and if available request location
                if (criteria != null)
                    provider = locationManager.getBestProvider(criteria, true);
                if (provider != null && locationManager.isProviderEnabled(provider)){
                    try{
                        locationManager.removeUpdates(singeUpdateListener); //remove any previous updates
                        locationManager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, singeUpdateListener, context.getMainLooper());
                    }catch (IllegalArgumentException e){
                        throw new NoProviderAvailableException(e);
                    }
                    //if there is a timeout specified, start a handler to cancel
                    if (maxFixTime > 0){
                        if (handler != null)
                            handler.removeCallbacksAndMessages(null);
                        else
                            handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cancel();
                                if (maxRetries > 0) {
                                    try {
                                        getLocation(minDistance, minTime, maxFixTime, maxRetries - 1, l);
                                    } catch (NoProviderAvailableException e) {
                                        Log.e(TAG, "Provider disappeared: " + e.toString());
                                        if (singeUpdateListener != null){
                                            singeUpdateListener.onProviderDisabled(provider);
                                        }
                                    }
                                }
                            }
                        }, maxFixTime);
                    }
                }
                else
                    throw new NoProviderAvailableException();
            }
            return;
        } else if (singeUpdateListener != null){
            singeUpdateListener.onLocationChanged(bestResult);
        }

    }

    /**
     * This one-off {@link android.location.LocationListener} simply listens for a single location
     * update before unregistering itself.
     * The one-off location update is returned via the {@link android.location.LocationListener}
     */
    protected LocationListener singeUpdateListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Single Location Update Received: ");
            if (locationListener != null )
                locationListener.onLocationChanged(location);
            locationManager.removeUpdates(singeUpdateListener);
            if (handler != null)
                handler.removeCallbacksAndMessages(null);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (locationListener != null)
                locationListener.onStatusChanged(provider,status, extras);
        }

        public void onProviderEnabled(String provider) {
            if (locationListener != null)
                locationListener.onProviderEnabled(provider);
        }

        public void onProviderDisabled(String provider) {
            if (locationListener != null)
                locationListener.onProviderDisabled(provider);
        }
    };

    /**
     * {@inheritDoc}
     */
    public void cancel() {
        Log.d(TAG, "cancelling location request");
        if (singeUpdateListener != null)
            singeUpdateListener.onLocationChanged(null);
        locationManager.removeUpdates(singeUpdateListener);
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }
}
