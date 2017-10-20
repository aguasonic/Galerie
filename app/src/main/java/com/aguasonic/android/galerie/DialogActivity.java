//- ~ ©2015 Aguasonic Acoustics ~
package com.aguasonic.android.galerie;

//- Probably the smallest class you will ever see in Android. :D
import android.app.Activity;
import android.os.Bundle;

//- Only here to post our dialog.
final public class DialogActivity extends Activity {
    //private String LOG_TAG = getClass().getSimpleName();

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dialog);

        //- Because it's a fragment, MUST have constructor without arguments.
        final ConfirmDownload the_request = new ConfirmDownload();
        final int screen_metrics = AppState.get_screen_metric();
        final String the_tag = "" + screen_metrics;

        //- Ask the user for permission to get the rest.
        the_request.show(getFragmentManager(), the_tag);
    }
}

//- ~ ©2015 Aguasonic Acoustics ~
