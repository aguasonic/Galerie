//- ~ ©2015 Aguasonic Acoustics ~
package com.aguasonic.android.galerie.data;

//- Android code.
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Manages a local database for AGUASONIC thumbnails.
 */
final public class GD_DAO extends SQLiteOpenHelper {
    private final String LOG_TAG = this.getClass().getSimpleName();

    //- If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 110;
    //- Resource strings.
    private static final String DATABASE_NAME = "galerie_data.db";
    private Context the_context;

    //- Only called once, so we should set a preference.
    //- Returns _true_ on success, _false_ otherwise.
    private void write_initial_load(final SQLiteDatabase the_db,
                                final ContentValues[] the_values) {
        final String banner = "=======================================================";
        final String err_msg = ")))))))))))))))))) _pre-load failed_ ((((((((((((((((((";
        final int n_values = the_values.length;
        int returnCount = 0;

        //- If true, this is sooooo not cool.
        if (n_values < 1) {
            Log.e(LOG_TAG, banner);
            Log.w(LOG_TAG, err_msg);
            Log.e(LOG_TAG, banner);
        }

        //- Okay, insert the initial load.
        the_db.beginTransaction();

        try {
            for (final ContentValues this_value : the_values) {
                final long return_value =
                        the_db.insertWithOnConflict(GD_Contract.GD_Entry.TABLE_NAME,
                                null, this_value, SQLiteDatabase.CONFLICT_IGNORE);

                //- It's an empty database, so this should never happen --
                //- <but>, if it does we need to be aware of the issue.
                if (return_value != -1)
                    returnCount++;
            }

            //- Mark our activity a success.
            the_db.setTransactionSuccessful();
        } finally {
            the_db.endTransaction();
        }

        //- This is a load from existing resources, so if they
        //- are not /all/ loaded it's kind of a big deal.
        if (n_values != returnCount) {
            Log.e(LOG_TAG, banner);
            Log.w(LOG_TAG, err_msg);
            Log.e(LOG_TAG, banner);
        }
    }

    public GD_DAO(final Context this_context) {
        super(this_context, DATABASE_NAME, null, DATABASE_VERSION);

        if (the_context == null)
            the_context = this_context;
    }

    //- From the docs...
    // Called when the database is created for the first time.
    // This is where the creation of tables and the initial population of the tables should happen.
    @Override
    public void onCreate(final SQLiteDatabase the_db) {
        //- Create a table to hold thumbnails and some information about them.
        //- Such information includes the year they were made and a reference
        //- to the source of the sound the image was made from.
        //- _ID is defined in BaseColumns
        final String SQL_CREATE_THUMBNAIL_TABLE = "CREATE TABLE " +
                GD_Contract.GD_Entry.TABLE_NAME + " (" +
                GD_Contract.GD_Entry._ID + " INTEGER PRIMARY KEY," +
                GD_Contract.GD_Entry.COLUMN_SOUND_SOURCE + " INTEGER NOT NULL, " +
                GD_Contract.GD_Entry.COLUMN_YEAR_MADE + " INTEGER NOT NULL, " +
                GD_Contract.GD_Entry.COLUMN_THUMBNAIL_PATH + " TEXT " +
                " );";
        final GD_Resources gd_resources = new GD_Resources(the_context);
        //- This maps our initial set of Drawables to Bitmaps
        //- that can be used by the ContentProvider.
        final ContentValues[] the_values = gd_resources.getValues();

        //-- Invoke our table creation statement.
        the_db.execSQL(SQL_CREATE_THUMBNAIL_TABLE);

        //- An empty database isn't that useful.
        //- These will be overwritten by thumbnails that have a
        //- resolution determined by the device characteristics.
        write_initial_load(the_db, the_values);
    }


    //- Must be implemented. Even if all we do is drop it and start over.
    @Override
    public void onUpgrade(final SQLiteDatabase the_db,
                          final int oldVersion,
                          final int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        the_db.execSQL("DROP TABLE IF EXISTS " + GD_Contract.GD_Entry.TABLE_NAME);

        onCreate(the_db);
    }
}

//- ~ ©2015 Aguasonic Acoustics ~
