/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2013 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.android.plm.client.GCM;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.docdoku.android.plm.network.HTTPDeleteTask;
import com.docdoku.android.plm.network.HTTPPutTask;
import com.docdoku.android.plm.network.HTTPResultTask;
import com.docdoku.android.plm.network.listeners.HTTPTaskDoneListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * {@code Service} that handles the GCM Id.
 * <p>
 * Checks if a valid GCM exists, and if not sends a request to the GCM servers to request a new GCM.
 * <p>
 * Passes the GCM Id to the server to receive GCM messages or erases the GCM Id from the server to stop receiving
 * GCM messages. The action to be performed is specified in the {@code Intent Extra}.
 *
 * @version 1.0
 * @author: Martin Devillers
 */
public class GCMRegisterService extends Service {
    /**
     * Key for the {@code Intent Extra} indicating which action to perform
     */
    public static final  String INTENT_KEY_ACTION                       = "register/unregister";
    /**
     * Value of the {@code Intent Extra} indicating that the GCM Id should be sent to the server
     */
    public static final  int    ACTION_SEND_ID                          = 1;
    /**
     * Value of the {@code Intent Extra} indicating that the GCM Id should be removed from the server
     */
    public static final  int    ACTION_ERASE_ID                         = 2;
    private static final String LOG_TAG                                 = "com.docdoku.android.plm.client.GCM.GCMRegisterService";
    private static final String PREFERENCES_GCM                         = "GCM";
    private static final String PREFERENCE_KEY_GCM_ID                   = "gcm id";
    private static final String PREFERENCE_KEY_GCM_REGISTRATION_VERSION = "gcm version";
    private static final String PREFERENCE_KEY_GCM_EXPIRATION_DATE      = "gcm expiration";

    /**
     * This id can be found in the url of when connected in a navigator to the Google API Console.
     */
    private static final String SENDER_ID                   = "263093437022";
    /**
     * Default lifespan (7 days) of a reservation until it is considered expired. If the GCM Id stored on the phone is
     * more than a week old, that a new one will be queried, to make sure it hasn't changed.
     */
    private static final long   REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;

    private static final String JSON_KEY_GCM_ID = "gcmId";

    /**
     * Called when this {@code Service} is started.
     * <p>
     * Depending on the {@code Extra}s contained in the {@code Intent}, calls {@link #getGCMId()} to send the
     * GCM Id to the server or {@link #deleteGCMId()} to delete the GCM Id from the server.
     *
     * @param intent the intent generated by the gcm message
     * @param i
     * @param j
     * @return
     * @see Service
     */
    @Override
    public int onStartCommand(Intent intent, int i, int j) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            int action = intent.getExtras().getInt(INTENT_KEY_ACTION);
            switch (action) {
                case ACTION_SEND_ID:
                    getGCMId();
                    break;
                case ACTION_ERASE_ID:
                    deleteGCMId();
                    break;
                default:
                    Log.w(LOG_TAG, "No code provided for GCM Registration service");
                    break;
            }
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    /**
     * Unused method. Useful if this {@code Service} was bound to an {@code Activity}, which it shouldn't be.
     *
     * @param intent
     * @return
     * @see Service
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Searches in {@code SharedPreferences} for a GCM Id to send to the server. If no GCM Id is found or if the GCM
     * Id is either expired or belonging to another version of the app, {@link #getNewGCMId} is called to ask the GCM server
     * for a new Id.
     */
    private void getGCMId() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_GCM, MODE_PRIVATE);
        String gcmId = preferences.getString(PREFERENCE_KEY_GCM_ID, null);
        Log.i(LOG_TAG, "Looking for gcm Id...");
        if (gcmId == null) {
            Log.i("com.docdoku.android.plm", "No gcm Id was found in storage");
            getNewGCMId();
        }
        else {
            try {
                int gcmAppVersion = preferences.getInt(PREFERENCE_KEY_GCM_REGISTRATION_VERSION, -1);
                long expirationTime = preferences.getLong(PREFERENCE_KEY_GCM_EXPIRATION_DATE, -1);
                if (isGCMIdExpired(expirationTime) || isGCMIdPreviousVersion(gcmAppVersion)) {
                    Log.i(LOG_TAG, "gcm Id belonged to previoud app version or was expired");
                    getNewGCMId();
                }
                else {
                    Log.i(LOG_TAG, "gcm Id found! " + gcmId);
                    sendGCMId(gcmId);
                }
            }
            catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "Could not get package name");
                e.printStackTrace();
            }
        }
    }

    /**
     * Start a {@link HTTPDeleteTask} to delete the GCM Id from the server.
     * <p>
     * This {@code GCMRegisterService} is used as an {@link com.docdoku.android.plm.network.listeners.HTTPTaskDoneListener}.
     */
    private void deleteGCMId() {
        new HTTPDeleteTask(new HTTPTaskDoneListener() {
            @Override
            public void onDone(HTTPResultTask result) {
                Log.i(LOG_TAG, "Result of GCM Id delete: " + result);
                //TODO handle a failure to unregister from GCM
                stopSelf();
            }
        }).execute("/api/accounts/gcm");
    }

    /**
     * Start an {@code AsyncTask} to connect to the GCM server to ask for a new GCM Id.
     * <p>
     * Once it is received, calls
     * {@link #saveGCMId(String) saveGCMId()} to store it in the {@code SharedPreferences} then
     * {@link #sendGCMId(String) sendGCMId()} to send it to the DocDokuPLM server.
     */
    private void getNewGCMId() {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... objects) {
                GoogleCloudMessaging googleCloudMessaging = GoogleCloudMessaging.getInstance(GCMRegisterService.this);
                try {
                    String gcmId = googleCloudMessaging.register(SENDER_ID);
                    Log.i(LOG_TAG, "gcm Id obtained: " + gcmId);
                    saveGCMId(gcmId);
                    sendGCMId(gcmId);
                }
                catch (IOException e) {
                    Log.e(LOG_TAG, "IOException when registering for gcm Id");
                    Log.e(LOG_TAG, "Exception message: " + e.getMessage());
                    e.printStackTrace();
                }
                catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_TAG, "Exception when trying to retrieve app version corresponding to new gcm Id");
                    e.printStackTrace();
                }
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }.execute();
    }

    /**
     * Compares the provided time to the current one.
     *
     * @param expirationTime expiring time: one week after the last GCM Id was queried
     * @return if the current time is before the expiring time
     */
    private boolean isGCMIdExpired(long expirationTime) {
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Compares the application version provided to the one installed on the device.
     *
     * @param gcmAppVersion the used when the GCM Id was queried
     * @return if that version equals the one of the application
     * @throws PackageManager.NameNotFoundException
     */
    private boolean isGCMIdPreviousVersion(int gcmAppVersion) throws PackageManager.NameNotFoundException {
        int currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        return currentVersion != gcmAppVersion;
    }

    /**
     * Sends the GCM Id to the server in an {@link HTTPPutTask}.
     * <p>
     * This {@code GCMRegisterService} is used as an {@link com.docdoku.android.plm.network.listeners.HTTPTaskDoneListener}.
     *
     * @param gcmId the GCM Id sent to the server
     */
    private void sendGCMId(String gcmId) {
        Log.i(LOG_TAG, "Sending gcm id to server");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSON_KEY_GCM_ID, gcmId);
//            new HttpPutTask(this).execute("/api/accounts/gcm", jsonObject.toString());
            HTTPPutTask task = new HTTPPutTask(new HTTPTaskDoneListener() {
                @Override
                public void onDone(HTTPResultTask result) {
                    Log.i(LOG_TAG, "Result of sending GCM Id to server: " + result.isSucceed());
                    //TODO handle a failure to register to GCM
                }
            });
            task.execute("/api/accounts/gcm", jsonObject.toString());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        stopSelf();
    }

    /**
     * Saves the GCM Id to the {@code SharedPreferences} along with the data about the current app version and the
     * current date.
     *
     * @param gcmId the GCM Id to be saved
     * @throws PackageManager.NameNotFoundException if the current app version could not be found
     */
    private void saveGCMId(String gcmId) throws PackageManager.NameNotFoundException {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_GCM, MODE_PRIVATE);
        preferences.edit()
                .putString(PREFERENCE_KEY_GCM_ID, gcmId)
                .putInt(PREFERENCE_KEY_GCM_REGISTRATION_VERSION, getPackageManager().getPackageInfo(getPackageName(), 0).versionCode)
                .putLong(PREFERENCE_KEY_GCM_EXPIRATION_DATE, System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS)
                .commit();
    }
}
