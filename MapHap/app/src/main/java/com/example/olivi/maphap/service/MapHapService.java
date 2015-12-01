package com.example.olivi.maphap.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.olivi.maphap.R;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by olivi on 11/18/2015.
 */

public class MapHapService extends IntentService {
    public static final String SEARCH_QUERY_EXTRA = "sqe";
    public static final String LATITUDE_QUERY_EXTRA = "latqe";
    public static final String LONGITUDE_QUERY_EXTRA = "longqe";
    public static final String WITHIN_QUERY_EXTRA = "wqe";
    public static final String EXPANSIONS_QUERY_EXTRA = "eqe";
    private final String LOG_TAG = MapHapService.class.getSimpleName();
    public MapHapService() {
        super("MapHap");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "onHandleIntent called.");
        String searchQuery = intent.getStringExtra(SEARCH_QUERY_EXTRA);
        double latitude = intent.getDoubleExtra(LATITUDE_QUERY_EXTRA, 0.00);
        String latitudeQuery = Double
                .toString(latitude);
        double longitude = intent.getDoubleExtra(LONGITUDE_QUERY_EXTRA, 0.00);
        String longitudeQuery = Double
                .toString(longitude);
        int within = intent.getIntExtra(WITHIN_QUERY_EXTRA, 50);
        String withinQuery = Integer.toString(within) + "mi";

        String expansions = intent.getStringExtra(EXPANSIONS_QUERY_EXTRA);

        Log.i(LOG_TAG, "expansions: " + expansions);

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String eventsJsonStr = null;


        try {
            // Construct the URL for the Eventbrite query
            // Possible parameters are avaiable at
            // https://www.eventbrite.com/developer/v3/endpoints/events/
            final String EVENTS_BASE_URL =
                    "https://www.eventbriteapi.com/v3/events/search/";
            final String SEARCH_QUERY_PARAM = "q";
            final String LATITUDE_PARAM = "location.latitude";
            final String LONGITUDE_PARAM = "location.longitude";
            final String WITHIN_PARAM = "location.within";
            final String EXPAND_PARAM = "expand";
            final String OAUTH_TOKEN = "token";

            Uri builtUri = Uri.parse(EVENTS_BASE_URL).buildUpon()
                    .appendQueryParameter(SEARCH_QUERY_PARAM, searchQuery)
                    .appendQueryParameter(LATITUDE_PARAM, latitudeQuery)
                    .appendQueryParameter(LONGITUDE_PARAM, longitudeQuery)
                    .appendQueryParameter(WITHIN_PARAM, withinQuery)
                    .appendQueryParameter(EXPAND_PARAM, expansions)
                    .appendQueryParameter(OAUTH_TOKEN, getString(R.string.my_personal_oauth_token))
                    .build();

            URL url = new URL(builtUri.toString());

            Log.i(LOG_TAG, "URL to access eventbrite: " + builtUri);

            // Create the request to Eventbrite, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            eventsJsonStr = buffer.toString();
            Log.i(LOG_TAG, "Response from Eventbrite API: " + eventsJsonStr);
            getEventsFromJson(eventsJsonStr, latitude, longitude, within, searchQuery);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getEventsFromJson(String eventJsonStr,
                                   double latitude,
                                   double longitude,
                                   int within,
                                   String searchTerm)
            throws JSONException {


        EventsDataJsonParser parser = new EventsDataJsonParser(eventJsonStr);
        parser.parse();


        ContentValues[] eventsContentValues =  parser.getEventsContentValues();

        //TODO add eventsContentValues to db via content provider.
        //Note that venues needs to be added first because the events table
        //references the venues. Venues is added in the getEventsContentValues() method

//            long locationId = addSearchRegion(latitude, longitude, within);
//            long searchId = addSearchTerm(searchTerm);
        //TODO create method to add search term to database and fix addSearchRegion


    }
}