package eu.danman.zidostreamer.zidostreamer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by ferencknebl on 2018. 03. 01..
 */

public class FFmpegWrapper {

    public interface ProcessListener {
        void onProcessFinished(FFmpegWrapper wrapper);
    }

    private Context context;
    private File fmBin;

    private Process ffmpegProcess = null;

    private StringBuffer errorlog = new StringBuffer();
    private StringBuffer outputlog = new StringBuffer();

    private Handler handler;

    public FFmpegWrapper(Context c) {
        context = c;
        handler = new Handler(Looper.getMainLooper());

        fmBin = new File(c.getFilesDir().getAbsolutePath(), "ffmpeg");
    }

    public boolean checkFFmpeg() {
        if (!fmBin.exists()){

            try {
                // TODO check ABI
                Util.copyFromStreamToFile(context.getAssets().open("armeabi-v7a/ffmpeg"), fmBin);

            } catch (IOException e) {
                Log.w(getClass().getSimpleName(), "Failed to copy ffmpeg", e);

                return false;
            }

            fmBin.setExecutable(true);
        }

        fmBin.setExecutable(true);

        return true;
    }

    public String getErrorLog() {
        return errorlog.toString();
    }

    public String getOutputLog() {
        return outputlog.toString();
    }

    public boolean version(ProcessListener listener) {
        try {
            launch("-version", null, listener);
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "Failed to get ffmpeg version", e);

            return false;
        }

        return true;
    }

    public void launch(String args, final InputStream instream, final ProcessListener listener) throws IOException {
        if (isProcessRunning()) {
            return;
        }

        ffmpegProcess = Runtime.getRuntime().exec(fmBin.getAbsolutePath() + " " + args);

        // create logger thread
        Thread errorlogThread= new Thread() {
            @Override
            public void run() {

                BufferedReader in = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()));

                errorlog.setLength(0);

                try {

                    while(isProcessRunning()) {

                        errorlog.append(in.readLine());

                        sleep(100);

                    }

                } catch (InterruptedException e) {

                } catch (IOException e) {

                }
            }
        };

        Thread outputlogThread= new Thread() {
            @Override
            public void run() {

                BufferedReader in = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));

                outputlog.setLength(0);

                try {

                    while(isProcessRunning()) {

                        outputlog.append(in.readLine());

                        sleep(100);

                    }

                } catch (InterruptedException e) {

                } catch (IOException e) {

                }

                if (listener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onProcessFinished(FFmpegWrapper.this);
                        }
                    });
                }
            }
        };

        if (instream != null) {
            Thread readerThread = new Thread() {
                @Override
                public void run() {

                    byte[] buffer = new byte[8192];
                    int read = 0;

                    OutputStream ffmpegInput = ffmpegProcess.getOutputStream();

                    try {

                        while (isProcessRunning()) {
                            if (instream.available() > 0) {
                                read = instream.read(buffer);
                                ffmpegInput.write(buffer, 0, read);
                            } else {
                                sleep(10);
                            }

                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            readerThread.start();
        }

        outputlogThread.start();
        errorlogThread.start();
    }

    public void stop(){
        if (ffmpegProcess != null){
            ffmpegProcess.destroy();
        }
        ffmpegProcess = null;
    }

    private boolean isProcessRunning(){

        try {
            ffmpegProcess.exitValue();
        } catch (IllegalThreadStateException e){
            return true;
        } catch (Exception e) {

        }

        return false;
    }
}
