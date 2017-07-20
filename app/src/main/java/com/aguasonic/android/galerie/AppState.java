//- ~ ©2015 Aguasonic Acoustics ~

package com.aguasonic.android.galerie;

import android.content.Context;
import android.content.SharedPreferences;

import android.util.Log;

//- Manages our Application State stored as Preferences.
//- Declaring as 'enum' is the best way to build a Singleton.
public enum AppState {
    SINGLETON_INSTANCE;

    private static final String LOG_TAG = "AppState";
    private static final String PREFERENCES_NAME = "Galerie_Data";
    //
    private static final String CONTENT_PROVIDER_LOADED = "Content_Provider_Loaded";
    private static final String APP_VERSION = "Application_Version";
    private static final String LAST_POSITION = "Last_Position";
    private static final String SCREEN_METRIC = "Screen_Metric";
    private static final String SORT_DIRECTION = "Sort_Direction";

    // private static final String THUMBNAIL_NR = "Thumbnail_";
    //- What direction to sort the results in.
    private static String SORT_ASCENDING = "ASC";
    private static String SORT_DESCENDING = "DESC";

    //- Only need to get a handle once.
    static private SharedPreferences the_data;

    static final public Context initialize(final Context app_context) {
        if (the_data == null) {
            //- Get our preferences handle.
            the_data = app_context.getSharedPreferences(
                    PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        return app_context;
    }

    //- On first run the Content Provider will not have data for a while
    //- so we present a 'default set' of thumbnails.
    static final public Boolean content_provider_is_ready() {

        return the_data.getBoolean(CONTENT_PROVIDER_LOADED, false);
    }

    //- On first run the Content Provider will not have data for a while.
    //- When we have finished our initial loading then we have data, and
    //- can say so.
    static final public void content_provider_set_loaded(final Boolean state_to_set) {
        final SharedPreferences.Editor the_editor = the_data.edit();

        the_editor.putBoolean(CONTENT_PROVIDER_LOADED, state_to_set);
        the_editor.apply();
    }

    //- Get the version of the application on file.
    static final public int get_app_version() {

        return the_data.getInt(APP_VERSION, 0);
    }

    //- Put the current version of the application on file.
    static final public void put_app_version(final int this_version) {
        final SharedPreferences.Editor the_editor = the_data.edit();

        the_editor.putInt(APP_VERSION, this_version);
        the_editor.apply();
    }

    //- Get the screen metric.
    static final public int get_screen_metric() {

        return the_data.getInt(SCREEN_METRIC, 0);
    }

    //- Put the current version of the application on file.
    static final public void put_screen_metric(final int this_metric) {
        final SharedPreferences.Editor the_editor = the_data.edit();

        the_editor.putInt(SCREEN_METRIC, this_metric);
        the_editor.apply();
    }

    //- Get the version of the application on file.
    static final public int get_last_position() {

        return the_data.getInt(LAST_POSITION, 0);
    }

    //- Put the current version of the application on file.
    static final public void put_last_position(final int this_position) {
        final SharedPreferences.Editor the_editor = the_data.edit();

        the_editor.putInt(LAST_POSITION, this_position);
        the_editor.apply();
    }

    //- Set our sort direction -- false is 'Ascending', true is 'Descending'
    static final public Boolean put_sort_direction(final Boolean sort_direction) {
        final SharedPreferences.Editor the_editor = the_data.edit();

        the_editor.putBoolean(SORT_DIRECTION, sort_direction);
        the_editor.apply();

        return sort_direction;
    }

    //- Set our sort direction -- false is 'Ascending', true is 'Descending'
    static final public String get_sort_direction() {

        if (the_data.getBoolean(SORT_DIRECTION, false))
            return SORT_DESCENDING;
        else
            return SORT_ASCENDING;

    }
}

//- ~ ©2015 Aguasonic Acoustics ~
