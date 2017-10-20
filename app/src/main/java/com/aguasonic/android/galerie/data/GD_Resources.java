/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */

package com.aguasonic.android.galerie.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.aguasonic.android.galerie.BitmapSupport;
import com.aguasonic.android.galerie.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mark on 7/18/15.
 */
final public class GD_Resources {
    private final String LOG_TAG = this.getClass().getSimpleName();
    private final int n_processors = Runtime.getRuntime().availableProcessors();
    private final ExecutorService the_es = Executors.newFixedThreadPool(n_processors * 2);
    //-
    //- The initial Drawable resources loaded until we get the rest.
    //- 42 is evenly divisible by 2, 3, 6 or 7, common numbers of columns.
    private final int[] initial_res_ids = {
            R.drawable.agua9985, R.drawable.agua9979, R.drawable.agua1447,
            R.drawable.agua1442, R.drawable.agua1438, R.drawable.agua1436,
            R.drawable.agua1435, R.drawable.agua1434, R.drawable.agua1433,
            R.drawable.agua1432, R.drawable.agua1431, R.drawable.agua1430,
            R.drawable.agua1429, R.drawable.agua1428, R.drawable.agua1427,
            R.drawable.agua1426, R.drawable.agua1425, R.drawable.agua1424,
            R.drawable.agua1423, R.drawable.agua1422, R.drawable.agua1421,
            R.drawable.agua1420, R.drawable.agua1419, R.drawable.agua1418,
            R.drawable.agua1417, R.drawable.agua1416, R.drawable.agua1415,
            R.drawable.agua1414, R.drawable.agua1413, R.drawable.agua1412,
            R.drawable.agua1411, R.drawable.agua1410, R.drawable.agua1409,
            R.drawable.agua1408, R.drawable.agua1407, R.drawable.agua1406,
            R.drawable.agua1405, R.drawable.agua1404, R.drawable.agua1403,
            R.drawable.agua1402, R.drawable.agua1401, R.drawable.agua1400
    };

    //- The 'aguasonic' identifiers for each drawable.
    private final int[] the_initial_ids = {
            9985, 9979, 1447, 1442, 1438, 1436,
            1435, 1434, 1433, 1432, 1431, 1430,
            1429, 1428, 1427, 1426, 1425, 1424,
            1423, 1422, 1421, 1420, 1419, 1418,
            1417, 1416, 1415, 1414, 1413, 1412,
            1411, 1410, 1409, 1408, 1407, 1406,
            1405, 1404, 1403, 1402, 1401, 1400
    };

    private final int n_ids = the_initial_ids.length;
    private final CountDownLatch the_cdl = new CountDownLatch(n_ids);
    //- Needed by the Bitmap code.
    private Context the_context;

    //- Take this id and run with it.
    private final class GD_PreloadCallable implements Callable<Void> {
        //private final String LOG_TAG = this.getClass().getSimpleName();
        private int this_agua_id;
        private int this_rsrc_id;
        private Context this_context;
        private CountDownLatch this_cdl;

        //- Constructor keeps an eye on how we were invoked for 'run' below.
        GD_PreloadCallable(final CountDownLatch the_cdl,
                           final Context the_context,
                           final int the_agua_id,
                           final int the_rsrc_id) {
            this_cdl = the_cdl;
            this_agua_id = the_agua_id;
            this_rsrc_id = the_rsrc_id;
            this_context = the_context;
        }

        //- Write our resource images to disk. Yes, it would be nice to get the resources
        //- from the ids, but they are _resources_, known at parse time, and it turns out
        //- to be an extraordinary deal to map a string { like
        //- String.format ("R.drawable.agua%04d", the_id) } to a resource. :(
        @Override
        public Void call() {
            //- Um. Why do we need our resources?
            final Resources the_res = this_context.getResources();
            //- Get a bitmap from this resource.
            final Bitmap the_bitmap = BitmapFactory.decodeResource(the_res, this_rsrc_id);

            //Log.e(LOG_TAG, "Writing bitmap for resource associated with " + this_agua_id + ".");

            //- Now that we have a Bitmap, write it to disk.
            BitmapSupport.writeThumbnailToDisk(this_context, the_bitmap, this_agua_id);

            //- Check this one off the list.
            this_cdl.countDown();

            return null;
        }
    }

    //- 'package-private' if you leave the access modifier off!
    GD_Resources(final Context app_context) {

        the_context = app_context;
    }

    //- 'package-private' if you leave the access modifier off!
    final ContentValues[] getValues() {
        final String msg_1 = "We have " + initial_res_ids.length + " ids to work with.";
        final String msg_2 = "Found " + n_processors + " processors.";
        final List<GD_PreloadCallable> the_callables = new ArrayList<>();

        //Log.w(LOG_TAG, "###############################################################");
        //Log.e(LOG_TAG, msg_1);
        //Log.e(LOG_TAG, msg_2);
        //Log.w(LOG_TAG, "###############################################################");

        //- Run through our list and create a task to go get each one.
        for (int the_idx = 0; the_idx < n_ids; the_idx++) {
            final int the_id = the_initial_ids[the_idx];
            final int the_res_id = initial_res_ids[the_idx];

            //- Build our list of tasks.
            the_callables.add(new GD_PreloadCallable(the_cdl, the_context, the_id, the_res_id));
        }

        try {
            //- Wish they had an 'invokeAll' for Runnables.
            the_es.invokeAll(the_callables);
        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
        }

        //- Wait until the latch is free { all tasks have completed }.
        try {
            the_cdl.await();
        } catch (final Exception the_ex) {
            Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
        }

        //- Release resources.
        the_es.shutdown();


        return (GD_Contract.getArrayOfContentValues(the_context, the_initial_ids));
    }

}

/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */
