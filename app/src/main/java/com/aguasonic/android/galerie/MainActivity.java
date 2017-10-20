//- ~ ©2014 Aguasonic Acoustics ~
package com.aguasonic.android.galerie;

//- To pick up some constants.

import com.aguasonic.android.galerie.data.GD_Contract;

import android.app.LoaderManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

//- View support.
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

//- The primary UI for the Galerie application.
final public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private String LOG_TAG = getClass().getSimpleName();
    //- The string describing the aguasonic image identifier..
    private final String EXTRA_AGUA_ID =
            "com.aguasonic.android.galerie.extra.AGUA_ID";
    //- The string describing the order in which to sort the results of a query.
    private final String CURRENT_POSITION = "Current_Position";
    //- Our loader id.
    private final int LOADER_ID = 1111;
    //
    //- Our application context. Straight out of the old X11 days...
    private Context app_context = null;
    //- Our adapter. Need to keep a handle so we can swap Cursors.
    private ThumbnailCursorAdapter the_tn_adapter = null;
    //- Our GridView. Need to keep a handle so we can scroll to the last position.
    private GridView the_grid = null;
    private Boolean cursor_not_loaded = true;
    //- Sometimes onResume shows up with no cursor -- so we delay our
    //- 'scroll to last position' until onLoadFinished shows up.
    private int go_on_finished;

    /*
     *
     *------------------------- Private classes.
     *
     *========================================================================
     *
     */
    //- Takes information held in our cursor and gets a Thumbnail.
    final private class ThumbnailCursorAdapter extends CursorAdapter {
        //- Reads the file indicated and returns the associated bitmap.
        private Bitmap readBitmapFromDisk(final String full_path) {
            final BitmapFactory.Options options = new BitmapFactory.Options();

            //- The normal Alpha-RGB format.
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            //- returns a Bitmap, of course.
            return (BitmapFactory.decodeFile(full_path, options));
        }

        /**
         * Public functions
         * ==============================================================
         */
        public ThumbnailCursorAdapter(final Context the_context,
                                      final Cursor the_cursor,
                                      final int the_flags) {
            super(the_context, the_cursor, the_flags);
        }

        @Override
        public void bindView(final View the_view,
                             final Context the_context,
                             final Cursor this_cursor) {
            //- Yes, we are presuming which column has the path.
            final String the_path = this_cursor.getString(1);

            ((ImageView) the_view).setImageBitmap(readBitmapFromDisk(the_path));
        }

        @Override
        public View newView(final Context the_context,
                            final Cursor the_cursor,
                            final ViewGroup the_parent) {

            final ImageView the_image_view = new ImageView(the_context);

            the_image_view.setScaleType(ImageView.ScaleType.CENTER);

            return the_image_view;
        }
    }

    /*
     *
     *------------------------- Private functions.
     *
     *========================================================================
     *
     */
    //- Put a message indicating we have new images { if we
    //- found any! } into a notification and post it.
    private void askToDownload(final Context the_context) {
        final Resources the_res = the_context.getResources();
        final String the_title = the_res.getString(R.string.title_question);
        final int NOTIFICATION_ID = 24;
        final String notifIdString = "" +NOTIFICATION_ID;
        final NotificationManager the_notification_manager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        final Intent the_intent = new Intent(this, DialogActivity.class);
        final PendingIntent pending_intent =
                PendingIntent.getActivity(this,
                        0, the_intent, PendingIntent.FLAG_ONE_SHOT);

        final NotificationCompat.Builder the_builder =
                new NotificationCompat.Builder(this, notifIdString)
                        .setAutoCancel(true)
                        .setContentTitle(the_title)
                        .setContentIntent(pending_intent)
                        .setSmallIcon(R.drawable.icon_question);

        //- Send it.
        the_notification_manager.notify(NOTIFICATION_ID, the_builder.build());
    }

    //- See if we are connected to a network of any kind.
    private Boolean we_are_connected() {
        final Context the_context = getApplicationContext();
        //- Only update if WiFi or 3G is connected and not roaming
        final ConnectivityManager the_cm =
                (ConnectivityManager) the_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo the_network_info = the_cm.getActiveNetworkInfo();

        //- Boolean flag indicates network state -- _but not how_ { WiFi or Cellular}.
        return ((the_network_info != null) && (the_network_info.isConnected()));
    }

    /*
     * There used to be a getBackgroundDataSetting()
     *
     * This method was deprecated in API level 14. As of ICE_CREAM_SANDWICH, availability of
     * background data depends on several combined factors, and this method will always return
     * true. Instead, when background data is unavailable, getActiveNetworkInfo() will now
     * appear disconnected.
    */
    private Boolean okay_to_download() {
        //- Everything below here acts on the basis of being connected and ready to go.
        if (we_are_connected()) {
            final Context the_context = getApplicationContext();
            //- Only update if WiFi or 3G is connected and not roaming
            final ConnectivityManager the_cm =
                    (ConnectivityManager) the_context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final TelephonyManager the_tel_mgr =
                    (TelephonyManager) the_context.getSystemService(Context.TELEPHONY_SERVICE);
            final NetworkInfo the_network_info = the_cm.getActiveNetworkInfo();

            //- Yes, these can come back null!
            if ((the_network_info != null) && (the_tel_mgr != null)) {
                //- Skip if no connection, or background data disabled
                final int net_type = the_network_info.getType();
                //- We don't really care about the sub-type at the moment.
                //final int sub_type = the_network_info.getSubtype();

                //- Mobile but not roaming, let's see what the user wants to do.
                if ((net_type == ConnectivityManager.TYPE_MOBILE) &&
                        !the_tel_mgr.isNetworkRoaming()) {
                    //- Post a notification to ask the user. Need to ask,
                    //- but this is on a timer, so main UI may not even be visible.
                    return false;

                } else if (net_type == ConnectivityManager.TYPE_WIFI) {

                    return true;
                }
            }
        }

        //- If we're here, we are either not connected, or need to ask permission to
        //- proceed in which case we will see the user wants to do and act accordingly.
        return false;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private int getAppVersion(final Context the_context) {
        try {
            final PackageInfo packageInfo = the_context.getPackageManager()
                    .getPackageInfo(the_context.getPackageName(), 0);

            return packageInfo.versionCode;

        } catch (final PackageManager.NameNotFoundException the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);

            //- should never happen
            throw new RuntimeException("Could not get package name: " + the_ex);
        }
    }

    /*---- LEAVE HERE -- Need it when we restore use of GCM.

    //- Returns true if we can support receipt of Google Cloud Messages.
    private boolean checkPlayServices() {

        final GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        final int play_status = googleAPI.isGooglePlayServicesAvailable(this);

        return (play_status == ConnectionResult.SUCCESS);
    }

    OR

    private boolean checkPlayServices() {
    final GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
    final int result = googleAPI.isGooglePlayServicesAvailable(this);

    if(result != ConnectionResult.SUCCESS) {
        if(googleAPI.isUserResolvableError(result)) {
            googleAPI.getErrorDialog(this, result,
                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
        }

        return false;
    }

    return true;
}
    */

    //- Lowest numbers are the oldest, mostly { highest are some of the
    //-  cartesians, which are old, but not many }. Rest are mandalas...
    //-
    private CursorLoader getCursorLoader(final String sortOrder) {
        final Uri the_uri = GD_Contract.GD_Entry.CONTENT_URI;
        final String the_projection[] = {
                GD_Contract.GD_Entry._ID,
                GD_Contract.GD_Entry.COLUMN_THUMBNAIL_PATH};
        final String the_selection = null;
        final String[] selection_args = null;

        return (new CursorLoader(app_context, the_uri,
                the_projection, the_selection, selection_args, sortOrder));
    }

    //- If we are mobile, _ask_ before you download a hundred MB!
    private void downloadTask(final Context the_context) {
        //- If it is okay, just do it -- else post a dialog and ask.
        if (okay_to_download())
            ThumbnailService.getThumbnails(the_context);
        else
            askToDownload(the_context);
    }

    private void finish_initialization(final int this_version) {
        //- Only update if WiFi or 3G is connected and not roaming
        final ConnectivityManager the_cm =
                (ConnectivityManager) app_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo the_network_info = the_cm.getActiveNetworkInfo();

        //- If both tasks completed, update to the current version and return.
        if (AppState.content_provider_is_ready()) {
            //- Both tasks have completed, so we're done.
            //- Write this version to persistent.
            AppState.put_app_version(this_version);

            return;
        }

        //- No network information, we can assume no network.
        if (the_network_info == null)
            return;

        //- Everything below here acts on the basis of being connected
        //- so if we're not connected, no point in bothering with the rest.
        if (!the_network_info.isConnected())
            return;

        //- Either never loaded, or did not complete load. If it is okay to download, do so.
        if (!AppState.content_provider_is_ready()) {
            final int TIMER_DOWNLOAD = 5000; // Five seconds
            final Handler the_handler = new Handler();
            final Runnable the_runnable = new Runnable() {
                public void run() {
                    downloadTask(getApplicationContext());
                }
            };

            the_handler.postDelayed(the_runnable, TIMER_DOWNLOAD);
        }
    }

    /*
     *
     *------------------------- Public functions.
     *
     *========================================================================
     *
     */
    //- UI Create
    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //- What version are we? Also performs AppState initialization.
        final int this_version = getAppVersion(getApplicationContext());

        //- Set up our AppState interface.
        if (app_context == null)
            app_context = AppState.initialize(getApplicationContext());

        //- These only need to be set once.
        if (AppState.get_app_version() < 100) {
            //- Need to know our screen metrics so we know which set of
            //-  thumbnails to request from the JSON_Fetch_Service
            final DisplayMetrics metrics = new DisplayMetrics();

            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            //- TODO Here as a placeholder until SettingsActivity shows up.
            AppState.put_sort_direction(true);

            //- So we can pass it on receipt of a GCM tickle.
            AppState.put_screen_metric(metrics.densityDpi);
        }


        //- Set our view.
        setContentView(R.layout.activity_main);

        //- Get our gridView.
        if (the_grid == null) {
            the_grid = (GridView) findViewById(R.id.gridview);

            //- onItemClick(AdapterView<?> parent, View the_view, int the_position, long the_id) {
            the_grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent,
                                        final View the_view,
                                        final int the_position,
                                        final long the_id) {

                    //- TODO -- check to see if we have the fullsized image
                    //- TODO corresponding to _the_id_ local to this device.
                    //-
                    //- TODO if we _do not_ ))and(( we are not connected, we need
                    //- TODO to let the user know.
                    if (we_are_connected()) {
                        final Intent the_intent =
                                new Intent(the_view.getContext(), FullsizeActivity.class);

                        //- Put the id of the selected image.
                        the_intent.putExtra(EXTRA_AGUA_ID, the_id);

                        //- Start the activity that presents the
                        //- selected thumbnail at full size.
                        startActivity(the_intent);
                    } else
                        //- We need the network to proceed, so remind the user.
                        Toast.makeText(getApplicationContext(),
                                R.string.network_reminder, Toast.LENGTH_SHORT).show();

                }
            });
        }

        //- If we don't have a cursor adapter, get one.
        if (the_tn_adapter == null) {
            the_tn_adapter = new ThumbnailCursorAdapter(this,
                    null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            //- Tell our GridView where the kids are.
            the_grid.setAdapter(the_tn_adapter);
        }

       /*----------------------------------------------------------------
        * This initializes the loader and makes it active. If the loader
        * specified by the ID already exists, the last created loader is reused.
        * If the loader specified by the ID does not exist, initLoader() triggers
        * the LoaderManager.LoaderCallbacks method onCreateLoader().
        *
        * Can pass our savedInstanceState if we like --
        * getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);
        *
        * but I have not seen any benefit in doing so at this time.
        */
        getLoaderManager().initLoader(LOADER_ID, null, this);

        //- Any time the application version number chances { either on first
        //- install, or upgrade } we need to request GCM client registration,
        //- per the specification.
        //-
        //- On the first time running, we need to go get the remaining thumbnails.
        //- When both tasks are completed, the version stored will be this version.
        //-
        if (this_version != AppState.get_app_version()) {
            final int TIMER_DELAY = 2000; //- Two seconds
            final Handler the_handler = new Handler();
            final Runnable the_runnable = new Runnable() {
                public void run() {
                    finish_initialization(this_version);
                }
            };

            //handler.postAtTime(runnable, System.currentTimeMillis()+interval);
            the_handler.postDelayed(the_runnable, TIMER_DELAY);
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu the_menu) {
        //- Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, the_menu);

        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem the_item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int selected_item = the_item.getItemId();

        switch (selected_item) {
            //-- We _might_ make this a WebFragment one day,
            //- action_settings is coming in the next release.
            case R.id.action_about: {
                //- Hard-coded because this isn't going anywhere.
                final String about_url = "http://android.aguasonic.com/about/";
                final Intent the_intent = new Intent(Intent.ACTION_VIEW);

                //-- Set the data for our call.
                the_intent.setData(Uri.parse(about_url));

                //-- Send our intent.
                startActivity(the_intent);
            }

            /*------------ TODO Coming soon.
            //- action_settings is coming in the next release.
            case R.id.action_settings: {
                final Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);

                Log.e(LOG_TAG, "onOptionsItemSelected -- chose action_settings");

                //- Start the activity that presents the
                //- selected thumbnail at full size.
                startActivity(intent);
            }
            */
        }

        return (super.onOptionsItemSelected(the_item));
    }

    /*
     *
     *------------------------- State Management functions.
     *
     *========================================================================
     *
     */
    @Override
    public final void onPause() {
        final int first_visible = the_grid.getFirstVisiblePosition();

        //- Save our current position.
        AppState.put_last_position((first_visible));

        //- Now we can call the super.
        super.onPause();
    }

    @Override
    public final void onRestoreInstanceState(final @NonNull Bundle previous_state) {
        super.onRestoreInstanceState(previous_state);
        final int last_position = previous_state.getInt(CURRENT_POSITION, 0);

        //- Go back to where we were.
        the_grid.smoothScrollToPosition(last_position);
    }

    @Override
    public final void onResume() {
        super.onResume();

        //- If we are coming up from scratch, but don't have a cursor, we
        //- need to tell 'loadFinished' to go back to our last position.
        if ((cursor_not_loaded) && (go_on_finished == 0)) {
            go_on_finished = AppState.get_last_position();
        }
    }

    @Override
    public final void onSaveInstanceState(final @NonNull Bundle the_state) {
        //- Where are we?
        final int first_visible = the_grid.getFirstVisiblePosition();

        //- In case program is being ended.
        AppState.put_last_position((first_visible));

        //- for the onRestoreInstanceState
        the_state.putInt(CURRENT_POSITION, first_visible);

        super.onSaveInstanceState(the_state);
    }

    /*
     *
     *------------------------- Loader Manager functions.
     *
     *========================================================================
     *
     * This creates and return a new Loader for the given ID.
     * This method returns the Loader that is created
     * There is no need to capture a reference to it.
     */
    @Override
    public final Loader<Cursor> onCreateLoader(final int the_loader_id, final Bundle the_bundle) {
        if (the_loader_id == LOADER_ID)
            return (getCursorLoader(AppState.get_sort_direction()));

        //- Should never get here { who else is calling our loader manager, and with
        //- what other LOADER_ID? } However, if we do, there is nothing to return.
        return null;
    }

    /**
     * This method is called when a previously created loader has finished its load.
     * This method is guaranteed to be called prior to the release of the last data that
     * was supplied for this loader. At this point you should remove all use of the old
     * data (since it will be released soon), but should not do your own release of the
     * data since its loader owns it and will take care of that.
     */
    @Override
    public final void onLoadFinished(final Loader<Cursor> the_loader, final Cursor the_cursor) {
        //- onLoadFinished, but no cursor passed in? How odd.
        if (the_cursor == null) {
            Log.e(LOG_TAG, "onLoadFinished -- but with a null cursor???");

            //- Can't do much with a null cursor.
            return;
        }

        //- Everything else has been done, just need to update the Cursor.
        the_tn_adapter.swapCursor(the_cursor);

        //- Cursor has changed, so tell the adapter.
        the_tn_adapter.notifyDataSetChanged();

        //- We have a cursor.
        cursor_not_loaded = false;

        if (go_on_finished > 0) {
            the_grid.smoothScrollToPosition(go_on_finished);

            //- Clear the flag.
            go_on_finished = 0;
        }
    }

    /**
     * This method is called when a previously created loader is being reset, thus making
     * its data unavailable. This callback lets you find out when the data is about to be
     * released so you can remove your reference to it.
     */
    @Override
    public final void onLoaderReset(final Loader<Cursor> the_cursor) {
        //- Tell our adapter the Cursor has been reset.
        //- Don't know how we would get reset before a load, but
        //- can't hurt to be sure our tn_adapter exists before we do
        //- something with it. :/
        if (the_tn_adapter != null)
            the_tn_adapter.swapCursor(null);

        //- Do not have a cursor.
        cursor_not_loaded = true;
    }
}

//- ~ ©2014 Aguasonic Acoustics ~
