package com.shaunzia.brainstaterecorder;

import com.interaxon.libmuse.ConnectionState;
import com.interaxon.libmuse.Muse;
import com.interaxon.libmuse.MuseConnectionPacket;
import com.interaxon.libmuse.MuseDataPacket;
import com.interaxon.libmuse.MuseDataPacketType;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * MainActivity.java corresponds to activity_main.xml and accesses SettingsActivity.java
 *
 * @author Shaun Zia
 * @version 2.0 27/11/2015
 */
public class MainActivity extends Activity {

    // Declare bluetooth connection variables
    public static final String TAG_CONNECT = "connect";
    public static final String TAG_DISCONNECT = "disconnect";

    // Declare record and stop variables
    public static final String TAG_RECORD = "record";
    public static final String TAG_STOP = "stop";

    // Declare chart view variables
    private ChartView gammaChartView;
    private ChartView betaChartView;
    private ChartView alphaChartView;
    private ChartView thetaChartView;
    private ChartView deltaChartView;

    // Declare connect and record buttons
    private Button connectAction;
    private Button recordAction;

    // Declare horseshoe variables
    private View tp9Status;
    private View fp1Status;
    private View fp2Status;
    private View tp10Status;

    // Declare Muse list variables
    private Spinner spinner;
    private List<Muse> currentMuses;

    // Declare csv logging variable
    private LoggingProcessor loggingProcessor;

    // Spinnner click listener
    private final View.OnClickListener connectClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.setEnabled(false);

            if (TAG_CONNECT.equals(v.getTag())) {
                // Connect
                MuseWrapper.getInstance(getApplicationContext())
                        .connect(currentMuses.get(spinner.getSelectedItemPosition()));
            } else {
                // Disconnect
                MuseWrapper.getInstance(getApplicationContext())
                        .disconnect(currentMuses.get(spinner.getSelectedItemPosition()));
            }
        }
    };

    // Record click listener
    private final View.OnClickListener recordClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.setEnabled(false);

            if (TAG_RECORD.equals(v.getTag())) {
                getDefaultLoggingProcessor().startLogging();
                if (getDefaultLoggingProcessor().isLogging()) {
                    v.setTag(TAG_STOP);
                    ((Button) v).setText(getString(R.string.btn_stop));
                }
                v.setEnabled(true);
            } else {
                getDefaultLoggingProcessor().stopLogging();
                if (!getDefaultLoggingProcessor().isLogging()) {
                    v.setTag(TAG_RECORD);
                    ((Button) v).setText(getString(R.string.btn_record));
                }
                v.setEnabled(true);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(v.getContext());
                dialogBuilder.setTitle(R.string.dialog_input_title);
                final EditText input = new EditText(v.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                dialogBuilder.setView(input);
                dialogBuilder.setPositiveButton(R.string.dialog_input_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getDefaultLoggingProcessor().renameRecords(input.getText().toString());
                    }
                });
                dialogBuilder.setNegativeButton(R.string.dialog_input_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialogBuilder.show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Display Muse drop-down list
        spinner = (Spinner) findViewById(R.id.spinner);

        // Establish a bluetooth connection
        connectAction = (Button) findViewById(R.id.connectbtn);
        recordAction = (Button) findViewById(R.id.recordbtn);
        connectAction.setOnClickListener(connectClickListener);
        recordAction.setOnClickListener(recordClickListener);

        // Locate horseshoe variables by ID
        tp9Status = findViewById(R.id.tp9indicator);
        fp1Status = findViewById(R.id.fp1indicator);
        fp2Status = findViewById(R.id.fp2indicator);
        tp10Status = findViewById(R.id.tp10indicator);

        // Define brain state chart views
        gammaChartView = (ChartView) findViewById(R.id.gammaview);
        betaChartView = (ChartView) findViewById(R.id.betaview);
        alphaChartView = (ChartView) findViewById(R.id.alphaview);
        thetaChartView = (ChartView) findViewById(R.id.thetaview);
        deltaChartView = (ChartView) findViewById(R.id.deltaview);

        // Define chart view labels
        gammaChartView.setChart("Gamma Wave (32-100 Hz) - Heightened Perception", getResources().getColor(R.color.md_red_500));
        betaChartView.setChart("Beta Wave (16-31 Hz) - Waking Consciousness", getResources().getColor(R.color.md_deep_purple_500));
        alphaChartView.setChart("Alpha Wave (8-15 Hz) - Deep Relaxation", getResources().getColor(R.color.md_indigo_500));
        thetaChartView.setChart("Theta Wave (4-7 Hz) - Dreamless Sleep", getResources().getColor(R.color.md_deep_orange_500));
        deltaChartView.setChart("Delta Wave (0.1-3 Hz) - Deep Sleep", getResources().getColor(R.color.md_blue_500));

    }

    // Refresh Muse devices using EventBus
    @Override
    protected void onResume() {
        super.onResume();
        refreshDevices();
        EventBus.getDefault().register(this);
    }

    // Pause data stream with EventBus
    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    // Establish data packet thread connection
    public void onEvent(final MuseConnectionPacket packet) {
        if (packet != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateStatus(packet.getCurrentConnectionState());
                }
            });
        }
    }

    // Create a thread for each brain state data packet
    public void onEvent(final MuseDataPacket packet) {
        if (packet != null && packet.getPacketType() == MuseDataPacketType.GAMMA_ABSOLUTE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Gamma", String.format("Gamma = %f", packet.getValues().get(0).floatValue()));
                    gammaChartView.addEntry(packet.getValues().get(0).floatValue());
                }
            });
        }
        if (packet != null && packet.getPacketType() == MuseDataPacketType.BETA_ABSOLUTE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Beta", String.format("Beta = %f", packet.getValues().get(0).floatValue()));
                    betaChartView.addEntry(packet.getValues().get(0).floatValue());
                }
            });
        }
        if (packet != null && packet.getPacketType() == MuseDataPacketType.ALPHA_ABSOLUTE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Alpha", String.format("Alpha = %f", packet.getValues().get(0).floatValue()));
                    alphaChartView.addEntry(packet.getValues().get(0).floatValue());
                }
            });
        }
        if (packet != null && packet.getPacketType() == MuseDataPacketType.THETA_ABSOLUTE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Theta", String.format("Theta = %f", packet.getValues().get(0).floatValue()));
                    thetaChartView.addEntry(packet.getValues().get(0).floatValue());
                }
            });
        }
        if (packet != null && packet.getPacketType() == MuseDataPacketType.DELTA_ABSOLUTE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Delta", String.format("Delta = %f", packet.getValues().get(0).floatValue()));
                    deltaChartView.addEntry(packet.getValues().get(0).floatValue());
                }
            });
        }
    }

    // Define horseshoe thread in arraylist
    public void onEvent(final ArrayList<Double> horseshoe) {
        if (horseshoe != null && horseshoe.size() == 4) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateHorseshoe(tp9Status, horseshoe.get(0).intValue());
                    updateHorseshoe(fp1Status, horseshoe.get(1).intValue());
                    updateHorseshoe(fp2Status, horseshoe.get(2).intValue());
                    updateHorseshoe(tp10Status, horseshoe.get(3).intValue());
                }
            });
        }
    }

    // Refresh Muse device list
    private void refreshDevices() {
        currentMuses = MuseWrapper.getInstance(getApplicationContext()).getPairedMused();
        List<String> spinnerItems = new ArrayList<String>();

        if (currentMuses == null || currentMuses.isEmpty()) {
            return;
        }
        for (Muse m : currentMuses) {
            String dev_id = m.getName() + "-" + m.getMacAddress();
            spinnerItems.add(dev_id);
        }

        ArrayAdapter<String> adapterArray = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerItems);
        spinner.setAdapter(adapterArray);

        ConnectionState state = currentMuses.get(spinner.getSelectedItemPosition()).getConnectionState();
        updateStatus(state);
    }

    // Bluetooth connection status
    private void updateStatus(ConnectionState state) {
        if (state == ConnectionState.CONNECTED) {
            /*connected*/
            connectAction.setText(R.string.btn_disconnect);
            connectAction.setTag(TAG_DISCONNECT);
            connectAction.setEnabled(true);

            recordAction.setText(R.string.btn_record);
            recordAction.setTag(TAG_RECORD);
            recordAction.setEnabled(true);

        } else if (state == ConnectionState.DISCONNECTED) {
            /*disconnected*/
            connectAction.setText(R.string.btn_connect);
            connectAction.setTag(TAG_CONNECT);
            connectAction.setEnabled(true);

            recordAction.setText(R.string.btn_record);
            recordAction.setTag(TAG_RECORD);
            recordAction.setEnabled(false);

        } else if (state == ConnectionState.CONNECTING) {
            /*connecting*/
            connectAction.setEnabled(false);
            recordAction.setEnabled(false);
        } else {
            connectAction.setText(R.string.btn_connect);
            connectAction.setTag(TAG_CONNECT);
        }
    }

    // Define horseshoe color coding scheme
    private void updateHorseshoe(View horseshoeView, int indicator) {
        int bgColor = getResources().getColor(R.color.md_indigo_200);
        switch (indicator) {
            case 1:
                bgColor = getResources().getColor(R.color.md_green_900);
                break;
            case 2:
                bgColor = getResources().getColor(R.color.md_green_700);
                break;
            case 3:
                bgColor = getResources().getColor(R.color.md_green_500);
                break;
            case 4:
                bgColor = getResources().getColor(R.color.md_green_200);
                break;
            default:
                break;
        }
        horseshoeView.setBackgroundColor(bgColor);
    }

    // csv logger
    private LoggingProcessor getDefaultLoggingProcessor() {
        if (loggingProcessor == null) {
            loggingProcessor = new LoggingProcessor();
        }
        return loggingProcessor;
    }

}
