package infrared5.com.red5proandroid;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import infrared5.com.red5proandroid.help.HelpDialogFragment;
import infrared5.com.red5proandroid.publish.Publish;
import infrared5.com.red5proandroid.subscribe.Subscribe;
import infrared5.com.red5proandroid.twoway.TwoWay;


public class Main extends Activity {

    DialogFragment helpDialog;

    public static Context mainContext;

    private void resetButtonGraphics() {
        ImageButton myButton = (ImageButton) findViewById(R.id.btnSubscribe);
        myButton.setImageResource(R.drawable.subscribe);
        myButton.invalidate();

        myButton = (ImageButton) findViewById(R.id.btnPublish);
        myButton.setImageResource(R.drawable.publish);
        myButton.invalidate();
    }

    private void startPublish() {

        ImageButton myButton = (ImageButton) findViewById(R.id.btnPublish);
        if(myButton!=null) {
            myButton.setImageResource(R.drawable.publish_grey);
            myButton.invalidate();
        }

        startActivity(new Intent(this, Publish.class));
    }

    private void startSubscribe() {

        ImageButton myButton = (ImageButton) findViewById(R.id.btnSubscribe);
        if(myButton!=null) {
            myButton.setImageResource(R.drawable.subscribe_grey);
            myButton.invalidate();
        }
        startActivity(new Intent(this,Subscribe.class));

    }

    private void startTwoWay() {
        ImageButton myButton = (ImageButton) findViewById(R.id.btnTwoWay);
        if(myButton!=null) {
            myButton.setImageResource(R.drawable.twoway);
            myButton.invalidate();
        }
        startActivity(new Intent(this,TwoWay.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainContext = this;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);

        final ImageButton pub = (ImageButton) findViewById(R.id.btnPublish);
        pub.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                startPublish();
            }
        });

        final ImageButton sub = (ImageButton) findViewById(R.id.btnSubscribe);
        sub.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                startSubscribe();
            }
        });

        final ImageButton twoway = (ImageButton) findViewById(R.id.btnTwoWay);
        twoway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTwoWay();
            }
        });

        final ImageButton help = (ImageButton) findViewById(R.id.btnHelp);
        help.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showHelp();
            }
        });

        helpDialog = HelpDialogFragment.newInstance();
    }

    private void showHelp() {
        helpDialog.show(getFragmentManager().beginTransaction(), "help_dialog");
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetButtonGraphics();
    }

    @Override
    public void onBackPressed() {
        resetButtonGraphics();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
