//- ~ 2015 Aguasonic Acoustics ~
package com.aguasonic.android.galerie.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.aguasonic.android.galerie.AppState;
import com.aguasonic.android.galerie.ThumbnailService;

//- Provides access to the Galerie Thumbnail Database.
final public class GD_Provider extends ContentProvider {
    private final String LOG_TAG = this.getClass().getSimpleName();
    private GD_DAO db_helper;

    // The URI Matcher used by this content provider.
    private static final UriMatcher the_URI_matcher = buildUriMatcher();

    //- Integers for matching URIs.
    private static final int THUMBNAILS = 100;
    private static final int THUMBNAILS_WITH_SOURCE = 200;
    private static final int THUMBNAILS_WITH_YEAR = 300;
    private static final int THUMBNAILS_WITH_SOURCE_AND_YEAR = 400;

    private static UriMatcher buildUriMatcher() {
        //- All paths added to the UriMatcher have a corresponding code to
        //- return when a match is found. The code passed into the constructor
        //- represents the code to return for the root URI.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = GD_Contract.CONTENT_AUTHORITY;

        //- For each type of URI we need a corresponding code.
        matcher.addURI(authority, GD_Contract.PATH_ALL, THUMBNAILS);
        matcher.addURI(authority, GD_Contract.PATH_BY_SOURCE + "/#", THUMBNAILS_WITH_SOURCE);
        matcher.addURI(authority, GD_Contract.PATH_BY_YEAR + "/#", THUMBNAILS_WITH_YEAR);
        matcher.addURI(authority, GD_Contract.PATH_BY_BOTH + "/#/#", THUMBNAILS_WITH_SOURCE_AND_YEAR);

        return matcher;
    }

    //- Take the values given and write them to the database.
    private int doBulkInsert(ContentValues[] the_values) {
        final SQLiteDatabase the_db = db_helper.getWritableDatabase();
        int returnCount = 0;

        //- Begin our transaction.
        the_db.beginTransaction();

        try {
            for (ContentValues this_value : the_values) {
                final long return_value =
                        the_db.insertWithOnConflict(GD_Contract.GD_Entry.TABLE_NAME,
                                null, this_value, SQLiteDatabase.CONFLICT_IGNORE);

                if (return_value != -1)
                    returnCount++;
            }

            //- Mark our activity a success.
            the_db.setTransactionSuccessful();
        } finally {
            the_db.endTransaction();
        }

        return returnCount;
    }

    //- Right now we just return everything we have. When the Settings Activity
    //- gets added we can choose by year, or source, or both.
    private Cursor getAllThumbnails(final String sortOrder) {
        final SQLiteDatabase the_db = db_helper.getReadableDatabase();
        final String chosenColumns[] = {
                GD_Contract.GD_Entry._ID,
                GD_Contract.GD_Entry.COLUMN_THUMBNAIL_PATH};
        //- Lowest are the oldest, mostly { highest are some of the cartesians,
        //- which are old, but not many }. Rest are mandalas...
        //final String sortOrder = GD_Contract.GD_Entry._ID + " DESC";

        //- No database, no cursor; else return what's there.
        if (the_db == null)
            return null;

        return (the_db.query(GD_Contract.GD_Entry.TABLE_NAME,
                chosenColumns, null, null, null, null, sortOrder));
    }

    //- A UriMatcher instance
    private static final UriMatcher uri_matcher = buildUriMatcher();


    @Override
    public boolean onCreate() {

        if (db_helper == null) {
            db_helper = new GD_DAO(getContext());

            //Log.e(LOG_TAG, "Opening: " + db_helper.getDatabaseName());
        }

        return true;
    }

    @Override
    public String getType(final Uri the_uri) {
        //- Use the Uri Matcher to determine what kind of URI this is.
        final int the_match = uri_matcher.match(the_uri);
        final String match_string = String.format("%d", the_match);

        switch (the_match) {
            //- All of them.
            case THUMBNAILS:
                return GD_Contract.GD_Entry.CONTENT_TYPE;

            //- Everything else will get done later { when we add the Settings Activity }
            // -- right now we just want them all.
            //- From a particular year -and- source.
            case THUMBNAILS_WITH_SOURCE:

                //- From a particular year.
            case THUMBNAILS_WITH_YEAR:

                //- From a particular year -and- source.
            case THUMBNAILS_WITH_SOURCE_AND_YEAR:

            default:
                Log.e(LOG_TAG, "=============== getType() defaulting ===============");
                Log.e(LOG_TAG, "getType(), the_uri: " + the_uri.toString());
                Log.e(LOG_TAG, "getType(), match is: " + match_string);

                throw new UnsupportedOperationException("Unknown uri: " + the_uri);
        }
    }

    @Override
    public Uri insert(final Uri the_uri, final ContentValues the_values) {

        Log.e(LOG_TAG, "insert uri: " + the_uri.toString());

        //- We do not allow insertions.
        throw new UnsupportedOperationException("No public inserts allowed.");
    }

    @Override
    public int update(final Uri the_uri, final ContentValues values,
                      final String selection, final String[] selectionArgs) {

        Log.e(LOG_TAG, "update uri: " + the_uri.toString());

        //- We do not allow updates.
        throw new UnsupportedOperationException("No public updates allowed.");
    }

    @Override
    public int delete(final Uri the_uri, final String selection, final String[] selectionArgs) {

        Log.e(LOG_TAG, "delete uri: " + the_uri.toString());

        //- We do not allow deletions.
        throw new UnsupportedOperationException("No deletions allowed.");
    }

    //- Query interface. 'the_direction' can be "ASC" or "DESC"
    @Override
    public Cursor query(final Uri the_uri, final String[] the_projection,
                        final String the_selection, final String[] the_selectionArgs,
                        final String the_direction) {
        final int the_match = the_URI_matcher.match(the_uri);
        final String the_sortOrder = GD_Contract.GD_Entry._ID + " " + the_direction;

        //- Here's the switch statement that, given a URI, will determine
        //- what kind of request it is, and query the database accordingly.
        //- TODO subsets based on source or year or both come when
        //- TODO we get the new SettingsActivity set up.
        switch (the_match) {
            //- "thumbnails/#"
            case THUMBNAILS_WITH_SOURCE:
                //retCursor = getThumbnailsBySource()
                Log.e(LOG_TAG, "query matched: THUMBNAILS_WITH_SOURCE");
                break;

            //- "thumbnails/#"
            case THUMBNAILS_WITH_YEAR:
                //retCursor = getThumbnailsByYear();
                Log.e(LOG_TAG, "query matched: THUMBNAILS_WITH_YEAR");
                break;

            //- "thumbnails/#/#"
            case THUMBNAILS_WITH_SOURCE_AND_YEAR:
                //retCursor = getThumbnailsBySourceAndYear();
                Log.e(LOG_TAG, "query matched: THUMBNAILS_WITH_SOURCE_AND_YEAR");
                break;

            //- "thumbnails" returns everything in the database.
            case THUMBNAILS: {
                final Cursor the_cursor = getAllThumbnails(the_sortOrder);
                final Context the_context = getContext();

                //- Lots of places for values to be null. :D
                if ((the_cursor != null) && (the_context != null)) {
                    final ContentResolver the_content_resolver =
                            the_context.getContentResolver();

                    if (the_content_resolver != null) {

                        //- Set our notification.
                        the_cursor.setNotificationUri(the_content_resolver, the_uri);

                        return the_cursor;
                    }
                }
            }
            break;

            default:
                Log.e(LOG_TAG, "<< NO MATCH >>");
        }

        //- If we got here, there is no match, and thus nothing to return.
        Log.e(LOG_TAG, "query with uri: " + the_uri.toString());
        return null;
    }

    @Override
    public int bulkInsert(final Uri the_uri, final @NonNull ContentValues[] values_to_insert) {
        if (values_to_insert.length > 0) {
            final int the_match = the_URI_matcher.match(the_uri);
            //- There are several URIs, yes -- but only one makes sense.
            final int returnCount =
                    (the_match == THUMBNAILS) ? doBulkInsert(values_to_insert) : 0;
            final Context the_context = AppState.initialize(getContext());

            //- If our returnCount is more than zero, let any listeners for this one know.
            if (returnCount > 0) {
                the_context.getContentResolver().notifyChange(the_uri, null);

                AppState.content_provider_set_loaded(true);
            }
            return returnCount;
        }

        //- If we get here we have issues.
        Log.e(LOG_TAG, "bulkInsert with no values??");

        return -1;
    }
}

//- ~ 2015 Aguasonic Acoustics ~
