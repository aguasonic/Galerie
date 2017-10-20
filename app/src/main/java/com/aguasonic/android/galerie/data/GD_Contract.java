//- ~ ©2015 Aguasonic Acoustics ~

package com.aguasonic.android.galerie.data;

import com.aguasonic.android.galerie.BitmapSupport;

//- Android support.
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;


/**
 * Defines table and column names for the
 * AGUASONIC Galerie Data database.
 */
final public class GD_Contract {

    //- The "Content authority" is a name for the entire content provider, similar
    //- to the relationship between a domain name and its website.  A convenient
    //- string to use for the content authority is the package name for the app,
    //- which is guaranteed to be unique on the device.
    static final String CONTENT_AUTHORITY = "com.aguasonic.android.galerie.provider";

    //- Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    //- the content provider.
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    //- The only valid path at this authority.
    static final String PATH_ALL = "thumbnails";
    static final String PATH_BY_SOURCE = "by_source";
    static final String PATH_BY_YEAR = "by_year";
    static final String PATH_BY_BOTH = "by_src_year";

    //- Inner class that defines the table contents of the location table
    //- GD is Galerie Data, of course.
    public static final class GD_Entry implements BaseColumns {
        //- 'package-private' data.
        static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_ALL;

        //- Table name
        static final String TABLE_NAME = "galerie_data";

        //- Integer Year the work was created.
        static final String COLUMN_YEAR_MADE = "the_year_made";

        //- Integer Source type of the sound the image was made from.
        static final String COLUMN_SOUND_SOURCE = "the_sound_source";

        //- Integer Source type of the sound the image was made from.
        public static final String COLUMN_THUMBNAIL_PATH = "the_thumbnail_path";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ALL).build();
    }

    //- Given our data, create a ContentValues instance that we can insert into the database.
    private static ContentValues getContentValuesEntry(final int the_id, final int the_source_tag,
                                                final int the_year_made, final String the_path) {

        final ContentValues the_values = new ContentValues();

        //- Unique id that references an AGUASONIC(R) image that is available for purchase.
        the_values.put(BaseColumns._ID, the_id);
        //- TODO Until we start parsing the JSON file.
        the_values.put(GD_Contract.GD_Entry.COLUMN_YEAR_MADE, the_year_made);
        //- TODO Until we start parsing the JSON file.
        the_values.put(GD_Contract.GD_Entry.COLUMN_SOUND_SOURCE, the_source_tag);
        //- Nope -- this is the actual path.
        the_values.put(GD_Contract.GD_Entry.COLUMN_THUMBNAIL_PATH, the_path);

        return the_values;
    }

    /**
     * Make an array of ContentValues suitable for an insert() call.
     * Eventually we will have arrays for the year made and source ids, also.
     *
     * @param the_context
     * @param the_ids
     * @return
     */
    static public final ContentValues[] getArrayOfContentValues(final Context the_context, final int[] the_ids) {
        //
        //- TODO TODO TODO
        //- Right now we are faking it until we can update the JSON format
        //- on the server. { 'src-tag' is there, but not the year made }.
        final int the_source_tag = 2;
        final int the_year_made = 2000;
        //
        final int n_values = the_ids.length;
        ContentValues[] array_of_values = new ContentValues[n_values];

        for (short idx = 0; idx < n_values; idx++) {
            final Integer this_id = the_ids[idx];
            final String the_path = BitmapSupport.getThumbnailPathFromID(the_context, this_id);

            array_of_values[idx] =
                    getContentValuesEntry(this_id, the_source_tag, the_year_made, the_path);
        }

        return array_of_values;
    }
}

//- ~ ©2015 Aguasonic Acoustics ~
