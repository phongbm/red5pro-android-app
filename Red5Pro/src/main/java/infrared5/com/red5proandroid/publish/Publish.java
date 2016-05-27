package infrared5.com.red5proandroid.publish;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.red5pro.streaming.R5Connection;
import com.red5pro.streaming.R5Stream;
import com.red5pro.streaming.R5StreamProtocol;
import com.red5pro.streaming.config.R5Configuration;
import com.red5pro.streaming.event.R5ConnectionEvent;
import com.red5pro.streaming.event.R5ConnectionListener;
import com.red5pro.streaming.source.R5AdaptiveBitrateController;
import com.red5pro.streaming.source.R5Camera;
import com.red5pro.streaming.source.R5Microphone;

import java.util.ArrayList;
import java.util.List;

import infrared5.com.red5proandroid.AppState;
import infrared5.com.red5proandroid.ControlBarFragment;
import infrared5.com.red5proandroid.R;
import infrared5.com.red5proandroid.settings.SettingsDialogFragment;

public class Publish extends Activity implements SurfaceHolder.Callback, View.OnClickListener,
        ControlBarFragment.OnFragmentInteractionListener, SettingsDialogFragment.OnFragmentInteractionListener {

    private int cameraSelection = 0;
    private int cameraOrientation = 0;
    private Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    private List<Camera.Size> sizes = new ArrayList<Camera.Size>();
    public static Camera.Size selected_size = null;
    public static String selected_item = null;
    public static int preferedResolution = 0;
    public static PublishStreamConfig config = null;
    protected boolean override = false;
    private R5Camera r5Cam;
    private R5Microphone r5Mic;
    protected SurfaceView surfaceForCamera;

    static {
        if(config==null){
            config = new PublishStreamConfig();
        }
    }

    protected Camera camera;
    protected boolean isPublishing = false;

    protected R5Stream stream;

    public final static String TAG = "Preview";

    public void onStateSelection(AppState state) {
        this.finish();
    }

    public void onSettingsClick() {
        openSettings();
    }

    public String getStringResource(int id) {
        return getResources().getString(id);
    }

    public int getIntResource(int id) {
        return getResources().getInteger(id);
    }

    public Boolean getBooleanResource(int id) {
        return getResources().getBoolean(id);
    }

    public void onSettingsDialogClose() {
        configure();
    }

    //grab user data to be used in R5Configuration
    protected void configure() {
        SharedPreferences preferences = getSharedPreferences(getStringResource(R.string.preference_file), MODE_MULTI_PROCESS);
        config.host = preferences.getString(getStringResource(R.string.preference_host), getStringResource(R.string.preference_default_host));
        config.port = preferences.getInt(getStringResource(R.string.preference_port), getIntResource(R.integer.preference_default_port));
        config.app = preferences.getString(getStringResource(R.string.preference_app), getStringResource(R.string.preference_default_app));
        config.name = preferences.getString(getStringResource(R.string.preference_name), getStringResource(R.string.preference_default_name));
        config.bitrate = preferences.getInt(getStringResource(R.string.preference_bitrate), Integer.parseInt(getStringResource(R.string.preference_default_bitrate)));
        config.audio = preferences.getBoolean(getStringResource(R.string.preference_audio), getBooleanResource(R.bool.preference_default_audio));
        config.video = preferences.getBoolean(getStringResource(R.string.preference_video), getBooleanResource(R.bool.preference_default_video));
        config.adaptiveBitrate = preferences.getBoolean(getStringResource(R.string.preference_adaptive_bitrate), getBooleanResource(R.bool.preference_default_adaptive_bitrate));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(override)
            return;

        //assign the layout view
        setContentView(R.layout.activity_publish);

        //setup properties in configuration
        configure();

        final View v = findViewById(android.R.id.content);

        v.setKeepScreenOn(true);

        ControlBarFragment controlBar = (ControlBarFragment)getFragmentManager().findFragmentById(R.id.control_bar);
        controlBar.setSelection(AppState.PUBLISH);
        controlBar.displayPublishControls(true);

        //activate the camera
        Camera.getCameraInfo(cameraSelection, cameraInfo);
        setOrientationMod();
        showCamera();

        ImageButton rButton = (ImageButton) findViewById(R.id.btnRecord);
        rButton.setOnClickListener(this);

        ImageButton cameraButton = (ImageButton) findViewById(R.id.btnCamera);
        cameraButton.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        openSettings();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopPublishing();
        stopCamera();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.publish, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            openSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSettings(){
        try {
            SettingsDialogFragment newFragment = SettingsDialogFragment.newInstance(AppState.PUBLISH);
            newFragment.show(getFragmentManager().beginTransaction(), "settings_dialog");

            List<String> sb = new ArrayList<String>();
            for(Camera.Size size:this.sizes){
                if((size.width/2)%16!=0){
                    continue;
                }

                String potential = String.valueOf(size.width).trim() +"x"+  String.valueOf(size.height).trim();
                sb.add(potential);
            }
            ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_spinner_item,sb) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    ((TextView) v).setTextColor(0xffff0000);
                    return v;
                }

                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);
                    ((TextView) v).setTextColor(0xffff0000);
                    ((TextView) v).setGravity(Gravity.CENTER);
                    return v;
                }
            };

            newFragment.setSpinnerAdapter(adapter);

        }
        catch(Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Can't open settings: " + e.getMessage());
        }
    }

    private void toggleCamera() {
        cameraSelection = (cameraSelection + 1) % 2;
        try {
            Camera.getCameraInfo(cameraSelection, cameraInfo);
            cameraSelection = cameraInfo.facing;
        }
        catch(Exception e) {
            // can't find camera at that index, set default
            cameraSelection = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        setOrientationMod();

        stopCamera();
        showCamera();
    }

    private void setOrientationMod(){

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_90:
                if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    degrees = 90;
                else
                    degrees = 270;
                break;
            case Surface.ROTATION_270:
                if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    degrees = 270;
                else
                    degrees = 90;
                break;
        }

        degrees += cameraInfo.orientation;

        cameraOrientation = degrees;
    }

    private void showCamera() {
        if(camera == null) {
            camera = Camera.open(cameraSelection);
            camera.setDisplayOrientation((cameraOrientation + (cameraSelection == Camera.CameraInfo.CAMERA_FACING_FRONT ? 180 : 0)) % 360);
            sizes=camera.getParameters().getSupportedPreviewSizes();
            SurfaceView sufi = (SurfaceView) findViewById(R.id.surfaceView);
            if(sufi.getHolder().isCreating()) {
                sufi.getHolder().addCallback(this);
            }
            else {
                sufi.getHolder().addCallback(this);
                this.surfaceCreated(sufi.getHolder());
            }
        }
    }

    private void stopCamera() {
        if(camera != null) {
            SurfaceView sufi = (SurfaceView) findViewById(R.id.surfaceView);
            sufi.getHolder().removeCallback(this);
            sizes.clear();

            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    //called by record button
    protected void startPublishing() {
        if(!isPublishing) {
            if(stream != null){
                stream.stop();
            }

            Handler mHand = new Handler();

            final R5Configuration configuration = new R5Configuration(R5StreamProtocol.RTSP, Publish.config.host,  Publish.config.port, Publish.config.app, 0.5f);
            stream = new R5Stream(new R5Connection(configuration));

            stream.setLogLevel(R5Stream.LOG_LEVEL_DEBUG);

            stream.connection.addListener(new R5ConnectionListener() {
                @Override
                public void onConnectionEvent(R5ConnectionEvent event) {
                    Log.d("publish","connection event code "+event.value()+"\n");
                    switch(event.value()){
                        case 0://open
                            break;
                        case 1://close
                            break;
                        case 2://error
                            break;

                    }
                }
            });

            stream.setListener(new R5ConnectionListener() {
                @Override
                public void onConnectionEvent(R5ConnectionEvent event) {
                    switch (event) {
                        case CONNECTED:
                            break;
                        case DISCONNECTED:
                            break;
                        case START_STREAMING:
                            break;
                        case STOP_STREAMING:
                            break;
                        case CLOSE:
                            break;
                        case TIMEOUT:
                            break;
                        case ERROR:
                            break;
                    }
                }
            });

            if(camera == null){
                camera = Camera.open(cameraSelection);
                Camera.getCameraInfo(cameraSelection, cameraInfo);
                setOrientationMod();
                camera.setDisplayOrientation((cameraOrientation + (cameraSelection == Camera.CameraInfo.CAMERA_FACING_FRONT ? 180 : 0)) % 360);
                sizes=camera.getParameters().getSupportedPreviewSizes();
            }

            camera.stopPreview();

            if(!override) {
                //assign the surface to show the camera output
                this.surfaceForCamera = (SurfaceView) findViewById(R.id.surfaceView);
                stream.setView((SurfaceView) findViewById(R.id.surfaceView));
            }

            //add the camera for streaming
            if(selected_item != null) {
                Log.d("publisher","selected_item "+selected_item);
                String bits[] = selected_item.split("x");
                int pW= Integer.valueOf(bits[0]);
                int pH=  Integer.valueOf(bits[1]);
                if((pW/2) %16 !=0){
                    pW=320;
                    pH=240;
                }
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(pW, pH);
                camera.setParameters(parameters);
                r5Cam = new R5Camera(camera,pW,pH);
                r5Cam.setBitrate(Publish.config.bitrate);
            }
            else {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(320, 240);

                camera.setParameters(parameters);
                r5Cam = new R5Camera(camera,320,240);
                r5Cam.setBitrate(config.bitrate);
            }

            r5Cam.setOrientation(cameraOrientation);
            r5Mic = new R5Microphone();

            if(config.video) {
                stream.attachCamera(r5Cam);
            }

            if(config.audio) {
                stream.attachMic(r5Mic);
            }

            if (config.adaptiveBitrate) {
                final R5AdaptiveBitrateController adaptiveBitrateController = new R5AdaptiveBitrateController();
                adaptiveBitrateController.AttachStream(stream);

                if (config.video) {
//                    adaptiveBitrateController.requiresVideo = true;
                }
            }

            isPublishing = true;
            stream.publish(Publish.config.name, R5Stream.RecordType.Live);
            camera.startPreview();

        }
    }

    private void stopPublishing() {
        if(stream!=null) {
            stream.stop();
        }
        isPublishing = false;
    }

    public void onClick(View view) {
        ImageButton rButton = (ImageButton) findViewById(R.id.btnRecord);
        ImageButton cameraButton = (ImageButton) findViewById(R.id.btnCamera);

        if(view.getId() == R.id.btnRecord) {
            if(isPublishing) {
                stopPublishing();
                rButton.setImageResource(R.drawable.empty_red);
                cameraButton.setVisibility(View.VISIBLE);

            }
            else {
                startPublishing();
                rButton.setImageResource(R.drawable.empty);
                cameraButton.setVisibility(View.GONE);
            }
        }
        else if(view.getId() == R.id.btnCamera) {
            toggleCamera();
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }
        catch(Exception e){
            e.printStackTrace();
        };

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {}

}
