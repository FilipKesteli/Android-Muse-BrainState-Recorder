package com.shaunzia.brainstaterecorder;

import com.interaxon.libmuse.MuseDataPacket;
import com.interaxon.libmuse.MuseDataPacketType;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * LoggingProcessor.java logs raw EEG signals and assigns a timestamp in milliseconds
 *
 * @author Shaun Zia
 * @version 2.0 27/11/2015
 */
public class LoggingProcessor {

    // Define file folder structure in Android
    public static final String FILE_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "MuseBrainState-recorder" + File.separator;

    // Declare brain state log variables
    public static final String PX_AG = "Absolute_Gamma";
    public static final String PX_AB = "Absolute_Beta";
    public static final String PX_AA = "Absolute_Alpha";
    public static final String PX_AT = "Absolute_Theta";
    public static final String PX_AD = "Absolute_Delta";
    public static final String EX = ".csv";

    // Assign HashMap to write files
    private HashMap<MuseDataPacketType, FileWriter> writers;

    // Declare brain state files
    private File absoluteGamma;
    private File absoluteBeta;
    private File absoluteAlpha;
    private File absoluteTheta;
    private File absoluteDelta;

    // Constructor
    public LoggingProcessor() {

    }

    // Start recording
    public void startLogging() {
        if (initWriters()) {
            EventBus.getDefault().register(this);
        }
    }

    // Stop recording
    public void stopLogging() {
        closeWriters();
        EventBus.getDefault().unregister(this);
    }

    // Write to column headers: timestamp & power spectral density corresponding to four EEG channels
    public void onEvent(final MuseDataPacket data) {
        if (writers != null && !writers.isEmpty()) {
            FileWriter writer = writers.get(data.getPacketType());

            if (writer != null) {
                try {
                    writer.write(
                            getTimestamp() + ","
                                    + data.getValues().get(0) + ","
                                    + data.getValues().get(1) + ","
                                    + data.getValues().get(2) + ","
                                    + data.getValues().get(3) + "\n");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // csv boolean logger
    public boolean isLogging() {
        return EventBus.getDefault().isRegistered(this);
    }

    // Define file path for csv records
    public boolean renameRecords(String note) {
        boolean absoluteGammaRename = false;
        boolean absoluteBetaRename = false;
        boolean absoluteAlphaRename = false;
        boolean absoluteThetaRename = false;
        boolean absoluteDeltaRename = false;

        if (absoluteGamma != null) {
            absoluteGammaRename = absoluteGamma.renameTo(new File(absoluteGamma.getAbsolutePath() + "_" + note + EX));
        }
        if (absoluteBeta != null) {
            absoluteBetaRename = absoluteBeta.renameTo(new File(absoluteBeta.getAbsolutePath() + "_" + note + EX));
        }
        if (absoluteAlpha != null) {
            absoluteAlphaRename = absoluteAlpha.renameTo(new File(absoluteAlpha.getAbsolutePath() + "_" + note + EX));
        }
        if (absoluteTheta != null) {
            absoluteThetaRename = absoluteTheta.renameTo(new File(absoluteTheta.getAbsolutePath() + "_" + note + EX));
        }
        if (absoluteDelta != null) {
            absoluteDeltaRename = absoluteDelta.renameTo(new File(absoluteDelta.getAbsolutePath() + "_" + note + EX));
        }
        return absoluteGammaRename && absoluteBetaRename && absoluteAlphaRename
                && absoluteThetaRename && absoluteDeltaRename;

    }

    // Write brain states to csv files
    private boolean initWriters() {
        File dir = makeDir();
        long currentTime = System.currentTimeMillis();

        absoluteGamma = new File(dir, currentTime + "_" + PX_AG);
        absoluteBeta = new File(dir, currentTime + "_" + PX_AB);
        absoluteAlpha = new File(dir, currentTime + "_" + PX_AA);
        absoluteTheta = new File(dir, currentTime + "_" + PX_AT);
        absoluteDelta = new File(dir, currentTime + "_" + PX_AD);

        try {
            writers = new HashMap<>();
            FileWriter absoluteGammaWriter = new FileWriter(absoluteGamma);
            FileWriter absoluteBetaWriter = new FileWriter(absoluteBeta);
            FileWriter absoluteAlphaWriter = new FileWriter(absoluteAlpha);
            FileWriter absoluteThetaWriter = new FileWriter(absoluteTheta);
            FileWriter absoluteDeltaWriter = new FileWriter(absoluteDelta);
            writers.put(MuseDataPacketType.GAMMA_ABSOLUTE, absoluteGammaWriter);
            writers.put(MuseDataPacketType.BETA_ABSOLUTE, absoluteBetaWriter);
            writers.put(MuseDataPacketType.ALPHA_ABSOLUTE, absoluteAlphaWriter);
            writers.put(MuseDataPacketType.THETA_ABSOLUTE, absoluteThetaWriter);
            writers.put(MuseDataPacketType.DELTA_ABSOLUTE, absoluteDeltaWriter);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Stop recording
    private boolean closeWriters() {
        if (writers != null && !writers.isEmpty()) {
            for (Map.Entry<MuseDataPacketType, FileWriter> next : writers.entrySet()) {
                if (next.getValue() != null) {
                    try {
                        next.getValue().flush();
                        next.getValue().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
            writers.clear();
        }
        return true;
    }

    // File directory structure
    private File makeDir() {
        File dir = new File(FILE_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    // Assign timestamp in milliseconds
    private String getTimestamp() {
        return System.currentTimeMillis() + "";
    }
}
