//- ~ ©2014 Aguasonic Acoustics ~

package com.aguasonic.android.galerie;


import android.app.Activity;
import android.app.WallpaperManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.res.Configuration;

//- View support.
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

//- for the actionBar 'share' support.
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

//- The Activity posted when a thumbnail is chosen.
final public class FullsizeActivity extends AppCompatActivity {
    private final String LOG_TAG = getClass().getSimpleName();
    private final String base_URL = "http://android.aguasonic.com/galerie/?";
    //- for the callback_??? methods below.
    private final Intent the_intent = new Intent(Intent.ACTION_VIEW);
    private int the_id_passed;

    //- See
    //- http://developer.android.com/reference/android/os/AsyncTask.html
    //- for reference.
    private class SetWallpaperTask extends AsyncTask<Void, Void, Integer> {
        private Drawable the_drawable;
        private Wallpaper_Info the_wp_info;

        private class Wallpaper_Info {
            //- Instantiated when the task is created.
            private Context the_context;
            private WallpaperManager the_wm;
            private int screen_height;
            private int screen_width;

            Wallpaper_Info(final Activity the_activity) {
                //- Instantiated when the task is created.
                the_context = the_activity.getBaseContext();
                the_wm = WallpaperManager.getInstance(the_context);
                screen_height = the_wm.getDesiredMinimumHeight();
                screen_width = the_wm.getDesiredMinimumWidth();
            }
        }

        public SetWallpaperTask(final Drawable this_drawable, final Activity the_activity) {
            the_drawable = this_drawable;
            the_wp_info = new Wallpaper_Info(the_activity);
        }

        //- We only need _one_ argument.
        @Override
        protected Integer doInBackground(final Void... imputed) {
            final int ret_failure = -1;

            //- If they are still viable.
            if ((the_drawable != null) && (the_wp_info != null)) {
                final Bitmap the_bitmap = ((BitmapDrawable) the_drawable).getBitmap();
                final int ret_success = 0;

                //- If we somehow failed to retrieve a bitmap, don't bother...
                if (the_bitmap != null) {
                    //- Comes back square, but that appears to be so
                    //- that it is not concerned with screen rotations?
                    final Bitmap new_bitmap = Bitmap.createScaledBitmap(the_bitmap,
                            the_wp_info.screen_width, the_wp_info.screen_height, true);

                    //- Now set it as the new wallpaper.
                    try {
                        the_wp_info.the_wm.setBitmap(new_bitmap);
                    } catch (final Exception the_ex) {
                        Log.e(LOG_TAG, the_ex.getMessage(), the_ex);
                    }

                    return ret_success;
                }
            }

            //- If we get here things did not go as expected.
            return ret_failure;
        }
    }


    //- Post our dialog.
    private void postDialog(final Activity the_activity, final ImageView the_view) {
        //- Somewhere in the update path this started crashing ~ aug/sep 2015...
        final AlertDialog.Builder the_builder = new AlertDialog.Builder(FullsizeActivity.this);

        the_builder.setTitle(R.string.title_question);
        the_builder.setMessage(getString(R.string.dialog_wallpaper));

        //- If yes then kick off wallpaper task.
        the_builder.setPositiveButton(R.string.affirm, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int button_id) {
                final ImageView the_image = (ImageView)
                        the_activity.findViewById(R.id.web_image);

                //- Need our web_image, and we need that drawable, or don't bother.
                if (the_image != null) {
                    final Drawable the_drawable = the_image.getDrawable();

                    if (the_drawable != null) {
                        final SetWallpaperTask wpTask =
                                new SetWallpaperTask(the_drawable, the_activity);

                        wpTask.execute();
                    } else
                        Log.e(LOG_TAG, "Could not find drawable????");
                } else
                    Log.e(LOG_TAG, "Could not find web_image????");
            }
        });

        //- If no, then do nothing.
        the_builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface the_dialog, final int button_id) {

                // User cancelled the dialog
                the_dialog.cancel();
            }
        });

        //- Create it. Show it.
        the_builder.create().show();
    }

    private void rotateIt(final ImageView the_iv) {
        final RotateAnimation anim_rotate =
                new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        //- Setup the animation.
        anim_rotate.setInterpolator(new LinearInterpolator());
        //- Go until we stop.
        anim_rotate.setRepeatCount(Animation.INFINITE);
        //- 3 times in 2 seconds.
        anim_rotate.setDuration(750);
        //- Start animation
        the_iv.startAnimation(anim_rotate);
    }

    //- Define it.
    private final class BitmapSaverTask extends AsyncTask<Void, Void, Void> {
        private Bitmap the_bitmap;
        private int the_id;

        public BitmapSaverTask(final Bitmap this_bitmap, final int this_id) {
            the_bitmap = this_bitmap;
            the_id = this_id;
        }

        //- Get the image.
        @Override
        protected Void doInBackground(final Void... imputed) {
            //- Save it for subsequent requests.
            if ((the_bitmap != null) && (the_id > 0))
                BitmapSupport.writeFullsizeToDisk(getApplicationContext(), the_bitmap, the_id);

            return null;
        }
    }


    //- Define it.
    private final class BitmapGetterTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageView the_iv;
        private int the_id;

        public BitmapGetterTask(final ImageView this_iv, final int this_id) {
            the_iv = this_iv;
            the_id = this_id;
        }

        //- Get the image.
        @Override
        protected void onPreExecute() {
            rotateIt(the_iv);
        }

        //- Get the image.
        @Override
        protected Bitmap doInBackground(final Void... imputed) {
            //- Go fetch the indicated PNG file and return a Bitmap.
            return (BitmapSupport.getFullsizeFromWeb(the_id));
        }

        //- Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(final Bitmap the_bitmap) {
            final BitmapSaverTask the_task =
                    new BitmapSaverTask(the_bitmap, the_id);

            //- Turn off the spinner.
            the_iv.setAnimation(null);

            //- Set the new image.
            the_iv.setImageBitmap(the_bitmap);

            //- Save a copy for later.
            the_task.execute();
        }
    }


    //- A helper for onCreate(), below.
    private void url_loader(final Context the_context, final int requested_id) {
        //- Get our web image viewer.
        final ImageView the_web_image = (ImageView) findViewById(R.id.web_image);
        //- The bounds for a valid aguasonic Image ID.
        final int minimum_id = 999;
        final int maximum_id = 10000;

        if ((requested_id > minimum_id) && (requested_id < maximum_id)) {
            //- Passed our range check, so now we can set it. This
            //-  one is used by the callback_??? functions below.
            the_id_passed = requested_id;

            //- If we already have it local...
            if (BitmapSupport.isFullsizeLoaded(the_context, requested_id)) {
                final Bitmap the_bitmap = BitmapSupport.readFullsizeFromDisk(the_context, requested_id);

                //- Set it.
                the_web_image.setImageBitmap(the_bitmap);

                //- Otherwise...
            } else {
                final BitmapGetterTask the_task =
                        new BitmapGetterTask(the_web_image, requested_id);

                //- Go get it.
                the_task.execute();
            }

            //- Handle long-clicks
            the_web_image.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View the_view) {

                    //- Which activity are we in?
                    final Activity the_act = FullsizeActivity.this;

                    postDialog(the_act, (ImageView) the_view);

                    //- Returning true means we handled the click.
                    return true;
                }
            });
        }
    }

    //- Set things up to share the selected image.
    private Intent createShareIntent() {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        final String base_URL = "http://android.aguasonic.com/galerie/shared/agua";
        //- We use PNG files for the thumbnails.
        final String the_suffix = ".png";
        //- Put together the full URL for the image.
        final String full_URL = base_URL + the_id_passed + the_suffix;

        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, full_URL);

        return shareIntent;
    }


/*
 *
 *------------------------- Public functions.
 *
 *========================================================================
 *
 */


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        //- The string describing the aguasonic image identifier..
        final String EXTRA_AGUA_ID =
                "com.aguasonic.android.galerie.extra.AGUA_ID";
        final Configuration the_config = getResources().getConfiguration();
        final long current_setup = the_config.orientation;

        super.onCreate(savedInstanceState);

        if (current_setup == Configuration.ORIENTATION_LANDSCAPE)
            setContentView(R.layout.activity_fullsize_l);
        else
            setContentView(R.layout.activity_fullsize_p);

        //- Load the intended image, set up the longClick callback...
        url_loader(getApplicationContext(), (int) getIntent().getLongExtra(EXTRA_AGUA_ID, (long) 0));
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu the_menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fullsize, the_menu);

        // Retrieve the share menu item
        final MenuItem the_share_item = the_menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        final ShareActionProvider the_provider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(the_share_item);

        // Attach an intent to this ShareActionProvider.  You can update this at any time,
        // like when the user selects a new piece of data they might like to share.
        if (the_provider != null) {
            the_provider.setShareIntent(createShareIntent());
        } else {
            Log.e(LOG_TAG, "==============================");
            Log.w(LOG_TAG, "Share Action Provider is null?");
            Log.e(LOG_TAG, "==============================");

            return false;
        }

        return true;
    }

    final public void callback_FMI(final View the_view) {
        //- 'for more information'
        final String key_fmi = "fmi=";
        final String full_URL = base_URL + key_fmi + the_id_passed;

        //-- Set the data for our call.
        the_intent.setData(Uri.parse(full_URL));

        //-- Send our intent.
        startActivity(the_intent);
    }

    final public void callback_POS(final View the_view) {
        //- 'point of sale'
        final String key_pos = "pos=";
        final String full_URL = base_URL + key_pos + the_id_passed;

        //-- Set the data for our call.
        the_intent.setData(Uri.parse(full_URL));

        //-- Send our intent.
        startActivity(the_intent);
    }

    final public void callback_TSO(final View the_view) {
        //- 'the sound of'
        final String key_tso = "tso=";
        final String full_URL = base_URL + key_tso + the_id_passed;

        //-- Set the data for our call.
        the_intent.setData(Uri.parse(full_URL));

        //-- Send our intent.
        startActivity(the_intent);
    }
}

//- ~ ©2014 Aguasonic Acoustics ~
