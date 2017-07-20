/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */

package com.aguasonic.android.galerie;

import com.aguasonic.android.galerie.data.GD_Contract;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;

//- StringSupport for parallel calls.
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

final public class ThumbnailService extends Service {
    private final String LOG_TAG = getClass().getSimpleName();
    private final int n_processors = Runtime.getRuntime().availableProcessors();
    private final ExecutorService the_es = Executors.newFixedThreadPool(n_processors * 2);

    //- If people get too carried away with starting and sliding it off the screen and re-starting
    //- we can end up with multiple 'onCreate()' calls from the UI, which want to go get thumbnails.
    //-
    //- So we set a gate that, once closed, won't be opened again as long as this process is running.
    //- or if we finish our tasks { at which point AppState.content_provider_has_data() is true. }
    //- BOTH conditions must evaluate to false in order to go get thumbnails.
    private static AtomicBoolean Thumbnail_Gate = new AtomicBoolean(false);
    //private static AtomicBoolean Preload_Gate = new AtomicBoolean(false);
    //
    //
    //- Public strings.
    //- It would be nice to build this from the package name -- but it bombs!
    //- final String the_package_name = getApplicationContext().getPackageName();
    //
    //- and this comes back with an empty string. Go figure.
    //- final String the_package_name = getPackageName();
    //
    //- If connected via WiFi, _or_ User says okay to get data over cell-phone link.
    //- 'bgs' is 'BackGround Service'. Not BundesGrenzSchultz. :D
    public static final String ACTION_BGS_GET_THUMBNAILS =
            "com.aguasonic.android.galerie.action.BGS_GET_THUMBNAILS";
    //
    //- On receipt of a GCM push, see which thumbnails are new and go get them.
    public static final String ACTION_BGS_ON_GCM =
            "com.aguasonic.android.galerie.action.BGS_ON_GCM";
    //- The Screen DPI.
    public static final String EXTRA_SCREEN_DPI =
            "com.aguasonic.android.galerie.extra.SCREEN_DPI";
    //- The Thumbnail Directory..
    public static final String EXTRA_THUMB_DIR =
            "com.aguasonic.android.galerie.extra.THUMB_DIR";

    //- The name of the array of thumbnails.
    private final String JSON_THUMBNAIL_LIST = "thumbnails";
    //- Name of the aguasonic identification numbers.
    private final String JSON_THUMBNAIL_ID = "id";

    //- Override our Digest Access class and provide a function
    //- for processing the body and returning a Bitmap.
    private final class PNG_DigestAccess extends DigestAccess {

        @Override
        public Object processResponseBody(final InputStream the_stream) {

            //- If we are using this correctly, the stream holds a PNG.
            return (BitmapFactory.decodeStream(the_stream));
        }
    }

    //- Override our Digest Access class and provide a function for processing the body.
    private final class Text_DigestAccess extends DigestAccess {
        @Override
        public Object processResponseBody(final InputStream the_stream) {
            final String the_encoding = "utf-8";

            try {
                //- Says faster than StringBuffer.
                final StringBuilder the_buffer = new StringBuilder();
                String the_line;

                //- Get a buffered reader for this stream.
                final BufferedReader the_reader =
                        new BufferedReader(new InputStreamReader(the_stream, the_encoding));
                //- Read until we're done.
                while ((the_line = the_reader.readLine()) != null)
                    the_buffer.append(the_line);

                if (the_buffer.length() > 0)
                    return (the_buffer.toString());

            } catch (final Exception the_ex) {
                Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
            }

            return null;

        }
    }

    //- Take this id and run with it.
    private final class Thumbnail_LoadTask implements Callable<Integer> {
        private final String LOG_TAG = getClass().getSimpleName();
        private int this_agua_id;
        private String this_folder;
        private Context this_context;


        //- Constructor keeps an eye on how we were invoked for 'run' below.
        Thumbnail_LoadTask(final Context the_context,
                           final String the_folder,
                           final int the_agua_id) {

            this_context = the_context;
            this_folder = the_folder;
            this_agua_id = the_agua_id;
        }

        private Bitmap getThumbnailFromID(final int req_id, final String sub_dir) {
            //- must END with path separator
            final String base_URL = "http://android.aguasonic.com/galerie/Thumbnails/";
            //- must BEGIN with path separator
            final String thumbnail_file = String.format("/%s.png", BitmapSupport.get_agua_key(req_id));
            final String complete_url = base_URL + sub_dir + thumbnail_file;
            //- This is the page we want to access.
            final PNG_DigestAccess the_test = new PNG_DigestAccess();

            //- Can fail for a lot of reasons...
            try {
                return ((Bitmap) the_test.run(complete_url, Credentials.password_thumbnails));
            } catch (final Exception the_ex) {
                Log.e(LOG_TAG, req_id + " : " + the_ex.getMessage(), the_ex);
            }

            return null;
        }

        //- Write our resource images to disk. Yes, it would be nice to get the resources
        //- from the ids, but they are _resources_, known at parse time, and it turns out
        //- to be an extraordinary deal to map a string { like
        //- String.format ("R.drawable.agua%04d", the_id) } to a resource. :(
        @Override
        public Integer call() {
            final Bitmap the_bitmap = getThumbnailFromID(this_agua_id, this_folder);

            //- If it came back not null, write it to disk and return 0 to indicate success.
            if (the_bitmap != null) {
                //- Write this bitmap to disk -- on failure, _we return the id_, else 0.
                if (BitmapSupport.writeThumbnailToDisk(this_context, the_bitmap, this_agua_id))
                    return 0;

            }

            //- If we get here we either could not get the bitmap, or could not write
            //- it to disk. By returning the id, we know which one to try again...
            return this_agua_id;
        }
    }

    //- Parse our JSON string, for each thumbnail get the identifier..
    private int[] get_IDs_from_JSON(final String jsonString) throws JSONException {
        final JSONObject thumbnailObject = new JSONObject(jsonString);
        final JSONArray thumbnailArray = thumbnailObject.getJSONArray(JSON_THUMBNAIL_LIST);
        //- If we have more than 65535, wow. We've been busy. :D
        final short n_thumbs = (short) thumbnailArray.length();
        final int[] array_of_ids = new int[n_thumbs];

        //- Run through our JSON string and pull out the aguasonic image identifiers.
        for (short idx = 0; idx < n_thumbs; idx++) {
            //- Get the JSON object representing the image.
            final JSONObject this_thumbnail = thumbnailArray.getJSONObject(idx);

            array_of_ids[idx] = this_thumbnail.getInt(JSON_THUMBNAIL_ID);
        }

        return array_of_ids;
    }

    //- Here because we do this on initial load, also.
    private int doBulkInsert(final int[] list_of_ids) {
        final Uri the_uri = GD_Contract.GD_Entry.CONTENT_URI;
        final ContentValues[] the_values =
                GD_Contract.getArrayOfContentValues(getApplicationContext(), list_of_ids);

        //Log.e(LOG_TAG, "doBulkInsert, values has length: " + the_values.length);

        return (getContentResolver().bulkInsert(the_uri, the_values));
    }

    //- Put a message indicating we have new images { if we
    //- found any! } into a notification and post it.
    private void sendNotification(final Context the_context) {
        final Resources the_res = the_context.getResources();
        final String the_title = the_res.getString(R.string.title_notification);
        final String the_msg = the_res.getString(R.string.text_notification);
        final int NOTIFICATION_ID = 11;
        final NotificationManager the_notification_manager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        final Intent the_intent = new Intent(this, MainActivity.class);
        //-- public static PendingIntent getActivity (Context context,
        //-- int requestCode, Intent intent, int flags)
        final PendingIntent the_pending =
                PendingIntent.getActivity(this, 0, the_intent, 0);

        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setContentText(the_msg)
                        .setContentTitle(the_title)
                        .setContentIntent(the_pending)
                        .setSmallIcon(R.drawable.icon_alert)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(the_msg));

        //- Send it.
        the_notification_manager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * Handle action handleActionLoadDB in the provided background
     * thread with the provided parameters.
     */
    private void populateDatabase(final String the_JSON_string) {
        int[] list_of_ids = null;

        try {
            //- TODO -- this will end up with three arrays, not just the id numbers.
            //- Parse our JSON string and get the list of thumbnail ID numbers.
            //- When the JSON on the server side gets fixed we'll return _three_
            //- arrays, the ids, source tag, -and- year made.
            list_of_ids = get_IDs_from_JSON(the_JSON_string);

        } catch (final JSONException the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
        } finally {
            if (list_of_ids != null) {
                //- This will also set content_provider_set_loaded() true.
                final int number_added = doBulkInsert(list_of_ids);

                //- If we just got a GCM to exercise the plumbing { no images
                //- actually added }, no point in posting a notification.
                if (number_added > 0) {

                    //- Tell the user.
                    sendNotification(getApplicationContext());

                    //-- Having completed its purpose, this service is no longer needed.
                    this.stopSelf();
                } else {
                    Log.i(LOG_TAG, "No notification because no images added.");

                    //- TODO Only here for testing GCM traffic -- REMOVE FOR RELEASE.
                    // sendNotification(getApplicationContext());
                }
            }
        }
    }


    /*
    int	DENSITY_280	Intermediate density for screens that sit between DENSITY_HIGH (240dpi) and DENSITY_XHIGH (320dpi).
    int	DENSITY_400	Intermediate density for screens that sit somewhere between DENSITY_XHIGH (320 dpi) and DENSITY_XXHIGH (480 dpi).
    int	DENSITY_560	Intermediate density for screens that sit somewhere between DENSITY_XXHIGH (480 dpi) and DENSITY_XXXHIGH (640 dpi).
    */

    private String get_sub_dir(final int screenDPI) {

        String thumbnail_sub_directory;

        //- Which sub-directory should we pull the thumbnails from?
        switch (screenDPI) {
            case DisplayMetrics.DENSITY_LOW:
                thumbnail_sub_directory = "res-ldpi";
                break;

            case DisplayMetrics.DENSITY_MEDIUM:
                thumbnail_sub_directory = "res-mdpi";
                break;

            case DisplayMetrics.DENSITY_HIGH:
                thumbnail_sub_directory = "res-hdpi";
                break;

            //- TODO: here until new files are posted.
            case DisplayMetrics.DENSITY_280:
            case DisplayMetrics.DENSITY_XHIGH:
                thumbnail_sub_directory = "res-xhdpi";
                break;

            //- TODO: here until new files are posted.
            case DisplayMetrics.DENSITY_400:
            case DisplayMetrics.DENSITY_560:
            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_XXXHIGH:
                thumbnail_sub_directory = "res-xxhdpi";
                break;

            default:
                Log.e(LOG_TAG, "====== Defaulting on screen density?! ======");

                //- Not sure how we would ever end up here...
                thumbnail_sub_directory = "res-hdpi";
        }

        return thumbnail_sub_directory;
    }

    //- Get the thumbnail that corresponds to each aguasonic_id in the list.
    private short go_get_thumbnails(final Context the_context,
                                    final String the_folder,
                                    final List<Integer> list_of_ids) {

        //-- One way to do it. final Set<Thumbnail_LoadTask> the_callables = new HashSet<>();
        final List<Thumbnail_LoadTask> the_callables = new ArrayList<>();
        short number_loaded = 0;

        //- We go until there is nothing left in the list.
        while (list_of_ids.size() > 0) {
            //- Run through our list and create a task to go get each one.
            for (int this_id : list_of_ids)
                the_callables.add(new Thumbnail_LoadTask(the_context, the_folder, this_id));

            try {
                final List<Future<Integer>> the_replies = the_es.invokeAll(the_callables);

                //- Clear the list.
                list_of_ids.clear();

                //- Parse our responses, add all non-zeros to the list for re-try.
                for (Future<Integer> this_reply : the_replies) {
                    final int this_response = this_reply.get();

                    //- If it comes back zero, it was successfully retrieved and written.
                    if (this_response == 0)
                        number_loaded++;
                        //- else, try again next time.
                    else
                        list_of_ids.add(this_response);
                }
            } catch (final Exception the_ex) {
                Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
            }
        }

        //- Ended with an empty list, so release resources.
        the_es.shutdown();

        return number_loaded;
    }

    //- Parse our JSON string, for each thumbnail get it and write to disk.
    private Boolean retrieveThumbnailsListedInJSON(final String jsonString, final String the_folder)
            throws JSONException {
        //
        final JSONObject thumbnailObject = new JSONObject(jsonString);
        final JSONArray thumbnailArray = thumbnailObject.getJSONArray(JSON_THUMBNAIL_LIST);
        //- If we have more than 65535, wow. We've been busy. :D
        final short n_thumbs = (short) thumbnailArray.length();
        final Context the_context = getApplicationContext();
        //- TODO These will  be parsed from the JSON string in the next release.
        final int the_source = 2;
        final int the_year = 2000;
        //
        //- We use a list because it can change with each pass.
        final List<Integer> the_list = new ArrayList<>();

        //- Parse the list and get the agua_id numbers. We only need to look up
        //- the id once, and the 'while' below might go more than one time.
        for (short idx = 0; idx < n_thumbs; idx++) {
            //- Get the JSON object representing the image.
            final JSONObject this_thumbnail = thumbnailArray.getJSONObject(idx);

            //- ID is on [1000...9999].
            the_list.add(this_thumbnail.getInt(JSON_THUMBNAIL_ID));
        }

        //- Spin off tasks to get our thumbnails.
        final short number_loaded = go_get_thumbnails(the_context, the_folder, the_list);

        //- IF number_marked == number_of_thumbnails, we're good.
        if (number_loaded == n_thumbs)
            return true;
        else
            Log.e(LOG_TAG, "Failed to load all of the thumbnails?!?");

        return false;
    }

    //- Take this id and run with it.
    private class get_JSON_Callable implements Callable<String> {
        @Override
        public String call() {
            //- This is the page we want to access.
            final Text_DigestAccess the_test = new Text_DigestAccess();
            //- Construct the URL for the JSON query asking for list of available thumbnails.
            final String the_URL = "http://android.aguasonic.com/galerie/Thumbnails/";

            //- Can fail for a lot of reasons...
            try {
                return ((String) the_test.run(the_URL, Credentials.password_thumbnails));
            } catch (final Exception the_ex) {
                Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
            }

            return null;
        }
    }

    //- We have our JSON string, now do the next step.
    private void processJSON_String(final String the_string,
                                    final int the_resolution_dpi) {
        final String the_sub_dir = get_sub_dir(the_resolution_dpi);

        try {
            //- by 'process' we mean retrieve the thumbnails
            //- corresponding to those listed in our JSON string
            //- and write them to local disk.
            if (retrieveThumbnailsListedInJSON(the_string, the_sub_dir))
                populateDatabase(the_string);
        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
        }

    }

    //- Even though this is a SERVICE <without> any kind of USER INTERFACE,
    //- performing a network activity on the 'main loop' { even though it is
    //- _not_ 'UI/Main' is still forbidden. So we have to background this kind
    //- of activity.
    private void get_JSON_String(final int the_dpi) {
        //- Just one. Without it, nothing else follows.
        final Future<?> the_future = the_es.submit((Callable<?>) new get_JSON_Callable());

        try {
            final String the_JSON_string = (String) the_future.get();

            //- If the string has non-zero length, send a message indicating that we finished!
            if ((the_JSON_string != null) && (the_JSON_string.length() > 0))
                processJSON_String(the_JSON_string, the_dpi);
        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, "Something happened waiting for the future!", the_ex);
        }
    }


    private void handleGetThumbnails(final Intent the_intent) {
        final int the_DPI = the_intent.getIntExtra(EXTRA_SCREEN_DPI, 0);

        if (!Thumbnail_Gate.get()) {

            //- Only one 'get thumbnails' is needed!
            Thumbnail_Gate.set(true);

            //- Go get our JSON string describing the available thumbnails.
            //- On success this starts a chain of tasks...
            get_JSON_String(the_DPI);
        }
    }


    //- TODO this gets finished when we re-code GCM sender.
    private void handleOnGcm(final Intent the_intent) {
        // final int the_DPI = the_intent.getIntExtra(EXTRA_SCREEN_DPI, 0);
        // final String the_dir = the_intent.getStringExtra(EXTRA_THUMB_DIR);

        Log.w(LOG_TAG, ")))) not yet implemented!");
        Log.e(LOG_TAG, ACTION_BGS_ON_GCM);
        Log.w(LOG_TAG, ")))) not yet implemented!");
    }


    //- no Binding needed for this one -- but we still have to implement onBind.
    @Override
    public IBinder onBind(final Intent the_intent) {
        throw new UnsupportedOperationException("No binding to this service.");
    }


    @Override
    public void onDestroy() {

        //- No longer need a thread pool.
        the_es.shutdown();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent the_intent,
                              final int the_flags,
                              final int start_ID) {

        if (the_intent != null) {
            final String the_requested_action = the_intent.getAction();

            if (the_requested_action != null)
                switch (the_requested_action) {
                    case ACTION_BGS_GET_THUMBNAILS:
                        //- Go get our thumbnails.
                        handleGetThumbnails(the_intent);
                        break;

                    case ACTION_BGS_ON_GCM:
                        //- When we receive a GCM Push, get a copy
                        //- of the JSON data, and compare to what we have.
                        handleOnGcm(the_intent);
                        break;
                }
        }

        //- With this return value, we are *not automatically re-invoked*.
        //- Only called from the UI, and run until told to end.
        return START_NOT_STICKY;
    }

    //- For get the thumbnails.
    static public void getThumbnails(final Context the_context) {
        if (the_context != null) {
            final Context app_context = AppState.initialize(the_context);

            if (!Thumbnail_Gate.get() && !AppState.content_provider_is_ready()) {
                final Intent the_intent = new Intent(app_context, ThumbnailService.class);
                final int the_DPI = AppState.get_screen_metric();
                final File the_dir_file = BitmapSupport.getThumbnailDirectory(app_context);
                final String the_dir = (the_dir_file == null) ? "" : the_dir_file.toString();

                Thumbnail_Gate.set(true);

                //- Set our action and data for this intent.
                the_intent.setAction(ThumbnailService.ACTION_BGS_GET_THUMBNAILS);
                the_intent.putExtra(ThumbnailService.EXTRA_SCREEN_DPI, the_DPI);
                the_intent.putExtra(ThumbnailService.EXTRA_THUMB_DIR, the_dir);

                //- Send this Intent to our ThumbnailService.
                the_context.startService(the_intent);
            } else {
                Log.e("ThumbnailService", ")))))))))))))) Gate is Closed ((((((((((((((");
                Log.e("ThumbnailService", "Gate is: " + (Thumbnail_Gate.get() ? "true" : "false"));
                Log.e("ThumbnailService", "CP is: " + (AppState.content_provider_is_ready() ? "true" : "false"));
            }
        }
    }
}

/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */
