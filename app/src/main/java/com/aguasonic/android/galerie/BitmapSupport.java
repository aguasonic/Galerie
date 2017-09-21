//- ~ ©2015 Aguasonic Acoustics ~

package com.aguasonic.android.galerie;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

//- StringSupport for handling image I/O.
//- Declaring as 'enum' is the best way to build a Singleton.
public enum BitmapSupport {
    SINGLETON_INSTANCE;
    private static final String LOG_TAG = "BitmapSupport";
    private static final String IMAGE_PREFIX = "agua";
    private static File local_fullsize_dir;
    private static File local_thumb_dir;

    //- Create the key to retrieve data for this value.
    static final public String get_agua_key(final int key_nr) {
        return (IMAGE_PREFIX + key_nr);
    }

    //- Has the Fullsize Bitmap for this id already been loaded? Let's see.
    static final public Boolean isFullsizeLoaded(final Context app_context, final int id_to_check) {
         final String filePath = getFullsizePathFromID(app_context, id_to_check);
         final File the_file = new File(filePath);

         return (the_file.exists());
    }

    //- Get the directory where thumbnails are stored...
    static final public File getThumbnailDirectory(final Context the_context) {
        if (local_thumb_dir == null) {
            final File the_base_dir = the_context.getFilesDir();
            final String base_path = the_base_dir.toString();
            final String dir_name = "/Thumbnails";
            final String full_path = base_path + dir_name;
            final File thumb_dir = new File(full_path);

            //- None of these warnings should be presented,
            //-  but here in development of the code.
            if (!thumb_dir.exists()) {
                if (!thumb_dir.mkdir()) {
                    Log.e(LOG_TAG, "Thumbnail Directory can not be made!");

                    return null;
                }
            }

            //- Directory made, but not a directory? How is that possible?
            if (!thumb_dir.isDirectory()) {
                Log.e(LOG_TAG, "Still not a directory?");

                return null;
            }

            //- If we got this far, keep a copy for subsequent calls.
            local_thumb_dir = thumb_dir;
        }

        return local_thumb_dir;
    }


    //- Get the directory where the full-sized images are stored...
    static final public File getFullsizeDirectory(final Context the_context) {
        if (local_fullsize_dir == null) {
            final File the_base_dir = the_context.getFilesDir();
            final String base_path = the_base_dir.toString();
            final String dir_name = "/Fullsize";
            final String full_path = base_path + dir_name;
            final File fullsize_dir = new File(full_path);

            //- None of these warnings should be presented,
            //-  but here in development of the code.
            if (!fullsize_dir.exists()) {
                if (!fullsize_dir.mkdir()) {
                    Log.e(LOG_TAG, "Fullsize Directory can not be made!");

                    return null;
                }
            }

            //- Directory made, but not a directory? How is that possible?
            if (!fullsize_dir.isDirectory()) {
                Log.e(LOG_TAG, "Still not a directory?");

                return null;
            }

            //- If we got this far, keep a copy for subsequent calls.
            local_fullsize_dir = fullsize_dir;
        }

        return local_fullsize_dir;
    }

    //- Given an id, what is the path to its thumbnail?
    static final public String getThumbnailPathFromID(final Context the_context, final int req_id) {
        final File the_directory = getThumbnailDirectory(the_context);
        final String thumbnail_file = String.format("/%s.png", get_agua_key(req_id));

        //- Return the full path of the thumbnail represented by this id number.
        return (the_directory != null) ? (the_directory.toString() + thumbnail_file) : null;
    }

    //- Reads the file indicated and returns the associated bitmap.
    //- Proper use means check to see if is there first!
    static final public Bitmap readFullsizeFromDisk(final Context the_context, final int req_id) {
        final String full_path = getFullsizePathFromID(the_context, req_id);
        final BitmapFactory.Options options = new BitmapFactory.Options();

        //- The normal Alpha-RGB format.
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try {
            return (BitmapFactory.decodeFile(full_path, options));
        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, req_id + " : " + the_ex.getMessage(), the_ex);
        }

        //- If we get here, somebody is not checking to see if
        //- the file exists before they ask for it. Or worse.
        return null;
    }

    //- Given an id, what is the path to its thumbnail?
    static final public String getFullsizePathFromID(final Context the_context, final int req_id) {
        final File the_directory = getFullsizeDirectory(the_context);
        final String fullsize_file = String.format("/%s.png", get_agua_key(req_id));

        //- Return the full path of the thumbnail represented by this id number.
        return (the_directory != null) ? (the_directory.toString() + fullsize_file) : null;
    }


    //- Writes the thumbnail image to our local folder.
    static final public Boolean writeThumbnailToDisk(final Context the_context,
                                               final Bitmap the_bitmap,
                                               final int the_file_id) {
        //- Can not use "try-with-resources" until _API 19_!
        BufferedOutputStream the_ostream = null;

        try {
            final File the_directory = getThumbnailDirectory(the_context);
            final String thumbnail_file = String.format("%s.png", get_agua_key(the_file_id));
            final int buffer_size = 8192; //- 8KB is fine.
            final OutputStream out_stream = new FileOutputStream(new File(the_directory, thumbnail_file));

            the_ostream = new BufferedOutputStream(out_stream, buffer_size);

            //- Now write to disk. 100 is the 'quality', but this
            // argument is both required and ignored here { PNG is lossless }.
            the_bitmap.compress(Bitmap.CompressFormat.PNG, 100, the_ostream);

            return true;

        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
        } finally {
            try {
                if (the_ostream != null) {
                    the_ostream.flush();
                    the_ostream.close();
                }
            } catch (final Exception the_ex) {
                Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
            }
        }

        return false;
    }

    //- Writes the fullsize image to our local folder.
    static final public void writeFullsizeToDisk(final Context the_context,
                                               final Bitmap the_bitmap,
                                               final int the_file_id) {
        //- Can not use "try-with-resources" until _API 19_!
        BufferedOutputStream the_ostream = null;

        try {
            final File the_directory = getFullsizeDirectory(the_context);
            final String thumbnail_file = String.format("%s.png", get_agua_key(the_file_id));
            final int buffer_size = 8192; //- 8KB is fine.
            final OutputStream out_stream = new FileOutputStream(new File(the_directory, thumbnail_file));

            the_ostream = new BufferedOutputStream(out_stream, buffer_size);

            //- Now write to disk. 100 is the 'quality', but this
            // argument is both required and ignored here { PNG is lossless }.
            the_bitmap.compress(Bitmap.CompressFormat.PNG, 100, the_ostream);

        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
        } finally {
            try {
                if (the_ostream != null) {
                    the_ostream.flush();
                    the_ostream.close();
                }
            } catch (final Exception the_ex) {
                Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
            }
        }
    }


    static final public Bitmap getFullsizeFromWeb(final int req_id) {
        //- must END with path separator
        final String base_URL = "http://android.aguasonic.com/galerie/Fullsize/";
        //- must BEGIN with path separator
        final String fullsize_file = String.format("%s.png", get_agua_key(req_id));
        final String complete_url = base_URL + fullsize_file;
        //- Override our Digest Access class and provide a function for processing the body.
        final class PNG_DigestAccess extends DigestAccess {

            @Override
            public final Object processResponseBody(final InputStream the_stream) {

                //- If we are using this correctly, the stream holds a PNG.
                return (BitmapFactory.decodeStream(the_stream));
            }
        }
        //- This is the page we want to access.
        final PNG_DigestAccess the_test = new PNG_DigestAccess();

        //- Can fail for a lot of reasons...
        try {
            return ((Bitmap) the_test.run(complete_url, Credentials.password_fullsize));
        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, req_id + " : " + the_ex.getMessage(), the_ex);
        }

        return null;
    }
}

//- ~ ©2015 Aguasonic Acoustics ~
