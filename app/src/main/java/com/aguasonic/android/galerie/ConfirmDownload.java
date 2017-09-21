//- ~ ©2014 Aguasonic Acoustics ~

package com.aguasonic.android.galerie;

//- Android code.
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

//- Confirmation Dialog when user does a 'long-press' on fullsized image.
final public class ConfirmDownload extends DialogFragment {
    private final String LOG_TAG = getClass().getSimpleName();

/*-- For version 1.14, 18feb2015, current thumbnail totals are:
50768	./res-xhdpi
111188	./res-xxhdpi
14176	./res-mdpi
8560	./res-ldpi
29576	./res-hdpi
*/

/*
int DENSITY_280	Intermediate density for screens that sit between DENSITY_HIGH (240dpi) and DENSITY_XHIGH (320dpi).
int	DENSITY_400	Intermediate density for screens that sit somewhere between DENSITY_XHIGH (320 dpi) and DENSITY_XXHIGH (480 dpi).
int	DENSITY_560	Intermediate density for screens that sit somewhere between DENSITY_XXHIGH (480 dpi) and DENSITY_XXXHIGH (640 dpi).
*/
    private String get_size_String(final int screenDPI) {
        final String the_format =  " { ~ %d MB }";

        //- Yes, these will change over time -- but will be known
        //- for each version at the time it is compiled.
        switch (screenDPI) {
            case DisplayMetrics.DENSITY_LOW:
                return (String.format(the_format, 9));

            case DisplayMetrics.DENSITY_MEDIUM:
                return (String.format(the_format, 14));

            case DisplayMetrics.DENSITY_HIGH:
                return (String.format(the_format, 30));

			//- TODO: here until new files are posted.
            case DisplayMetrics.DENSITY_280:
            case DisplayMetrics.DENSITY_XHIGH:
                return (String.format(the_format, 50));

            //- TODO: here until new files are posted.
            case DisplayMetrics.DENSITY_400:
            case DisplayMetrics.DENSITY_560:
            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_XXXHIGH:
                return (String.format(the_format, 111));

            default:
                Log.e(LOG_TAG, "<<< Defaulting on screen density?! >>>");
                return " { ~ ??? MB }";
        }
    }

    @Override
    public final Dialog onCreateDialog(final Bundle savedInstanceState) {
        //- Use the Builder class for convenient dialog construction
        final AlertDialog.Builder the_builder = new AlertDialog.Builder(getActivity());
        final String the_tag = getTag();
        final int the_screen_DPI = Integer.parseInt(the_tag);
        final Context the_context = getActivity();
        final String app_name = getString(R.string.app_name);
        final String additional_information = get_size_String(the_screen_DPI);
        final String the_question = getString(R.string.dialog_download) + additional_information;

        //- If yes then kick off wallpaper task.
        the_builder.setPositiveButton(R.string.affirm, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int button_id) {
                //- Kick off our background service to get the rest of the thumbnails.
                ThumbnailService.getThumbnails(the_context);

                //- Kind of a hack, because we came from a notification, which needed
                //- an activity to launch via intent, _not_ a fragment, which is where we are.
                getActivity().finish();
            }
        });

        //- If no, then do nothing.
        the_builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int button_id) {

                // User cancelled the dialog
                dialog.cancel();

                //- Kind of a hack, because we came from a notification, which needed
                //- an activity to launch via intent, _not_ a fragment, which is where we are.
                getActivity().finish();
            }
        });

        //- Post our question -- app name is here because there is no title.
        the_builder.setMessage(app_name + "\n\n" + the_question);

        //- Create the AlertDialog object and return it
        return (the_builder.create());
    }
}

//- ~ ©2014 Aguasonic Acoustics ~
