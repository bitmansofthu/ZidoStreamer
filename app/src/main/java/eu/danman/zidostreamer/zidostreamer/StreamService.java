package eu.danman.zidostreamer.zidostreamer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

public class StreamService extends Service {

    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;

    private SharedPreferences settings;

    private final IBinder mBinder = new LocalBinder();

    private MediaRecorder mMediaRecorder = null;

    private FFmpegCommandBuilder ffcmdbuilder;
    private FFmpegWrapper ffwrapper;
    private boolean ffmpegAvailable;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private int mScreenDensity;

    public class LocalBinder extends Binder {
        StreamService getService() {
            return StreamService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        ffcmdbuilder = new FFmpegCommandBuilder();
        ffwrapper = new FFmpegWrapper(this);

        ffmpegAvailable = ffwrapper.checkFFmpeg();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        // copy ffmpeg from sdcard to data folder
//        File fmBin = new File(getFilesDir().getAbsolutePath(), "ffmpeg");
//        fmBin.setExecutable(true);
//
//        // try cleanup first
//        stopFFMPEG();
//        releaseMediaRecorder();
//        releaseCamera();
//
//
//
//        // Start ffmpeg process
//        String cmd = settings.getString("ffmpeg_cmd", "");
//
//        Log.d("starting ffmpeg", cmd);
//
//        try {
//
//            ffmpegProcess = Runtime.getRuntime().exec(cmd);
//
//            final BufferedReader in = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()));
//
//            final Process thisFFMPEG = ffmpegProcess;
//
//            // create logger thread
//            Thread thread = new Thread() {
//                @Override
//                public void run() {
//
//                    String log = null;
//
//                    try {
//
//                        while(isProcessRunning(thisFFMPEG)) {
//
//                            log = in.readLine();
//
//                            if ((log != null) && (log.length() > 0)){
//                                Log.d("ffmpeg", log);
//                            } else {
//                                sleep(100);
//                            }
//
//                        }
//
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            };
//
//            thread.start();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return Service.START_NOT_STICKY;
//
//        } catch (RuntimeException e){
//            e.printStackTrace();
//            Toast.makeText(this, "You need to edit settings first!", Toast.LENGTH_LONG).show();
//            return Service.START_NOT_STICKY;
//        }
//
//        // get stream settings
//        int audio_bitrate;
//        int video_bitrate;
//        int video_framerate;
//        int video_size;
//        int video_width = 1920;
//        int video_height = 1080;
//
//        audio_bitrate =  Integer.parseInt(settings.getString("audio_bitrate", "128")) * 1024;
//        video_bitrate =  Integer.parseInt(settings.getString("video_bitrate", "4500")) * 1024;
//        video_framerate =  Integer.parseInt(settings.getString("video_framerate", "30"));
//        video_size = Integer.parseInt(settings.getString("video_size", "0"));
//
//        int cam_size;
//
//        switch (video_size){
//            default:
//            case 0:
//                cam_size = MCamera.Parameters.E_TRAVELING_RES_1920_1080;
//                video_width = 1920; video_height = 1080;
//                break;
//            case 1:
//                cam_size = MCamera.Parameters.E_TRAVELING_RES_1280_720;
//                video_width = 1280; video_height = 720;
//                break;
//            case 2:
//                cam_size = MCamera.Parameters.E_TRAVELING_RES_720_576;
//                video_width = 720; video_height = 576;
//                break;
//            case 3:
//                cam_size = MCamera.Parameters.E_TRAVELING_RES_720_480;
//                video_width = 720; video_height = 480;
//                break;
//            case 4:
//                cam_size = MCamera.Parameters.E_TRAVELING_RES_640_368;
//                video_width = 640; video_height = 368;
//                break;
//        }
//
//
//
//
//        // create proxy thread to read from mediarecorder and write to ffmpeg stdin
//        final ParcelFileDescriptor finalreadFD = readFD;
//
//        Thread readerThread = new Thread() {
//            @Override
//            public void run() {
//
//                byte[] buffer = new byte[8192];
//                int read = 0;
//
//                OutputStream ffmpegInput = ffmpegProcess.getOutputStream();
//
//                final FileInputStream reader = new FileInputStream(finalreadFD.getFileDescriptor());
//
//                try {
//
//                    while (true) {
//
//                        if (reader.available()>0) {
//                            read = reader.read(buffer);
//                            ffmpegInput.write(buffer, 0, read);
//                        } else {
//                            sleep(10);
//                        }
//
//                    }
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//
//                    stopSelf();
//                }
//            }
//        };
//
//        readerThread.start();

        return Service.START_NOT_STICKY;

    }

    public boolean configure(int resultCode, Intent resultData, int density) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjection = projectionManager.getMediaProjection(resultCode, resultData);

        mScreenDensity = density;

        return mMediaProjection != null;
    }

    public boolean start() {
        if (!ffmpegAvailable) {
            return false;
        }

        ffcmdbuilder.setUrl(settings.getString("stream_url", ""));

        //make a pipe containing a read and a write parcelfd
        ParcelFileDescriptor[] fdPair;
        try {
            fdPair = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            Log.w(getClass().getSimpleName(), "createPipe has failed", e);

            return false;
        }

        //get a handle to your read and write fd objects.
        ParcelFileDescriptor readFD = fdPair[0];
        ParcelFileDescriptor writeFD = fdPair[1];

        try {
            ffwrapper.launch(ffcmdbuilder.build(), new ParcelFileDescriptor.AutoCloseInputStream(readFD), new FFmpegWrapper.ProcessListener() {
                @Override
                public void onProcessFinished(FFmpegWrapper wrapper) {
                    teardown();
                }
            });
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "failed to launch ffmpeg", e);

            return false;
        }

        int video_bitrate =  Integer.parseInt(settings.getString("video_bitrate", "512")) * 1024;
        int video_framerate =  Integer.parseInt(settings.getString("video_framerate", "25"));

        mMediaRecorder = new MediaRecorder();

        // Step 2: Set sources
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);


        // set TS
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        mMediaRecorder.setAudioChannels(2);
//        mMediaRecorder.setAudioSamplingRate(44100);
//        mMediaRecorder.setAudioEncodingBitRate(audio_bitrate);0

        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        mMediaRecorder.setVideoFrameRate(video_framerate);
        mMediaRecorder.setVideoEncodingBitRate(video_bitrate);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(writeFD.getFileDescriptor());

        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "Failed to prepare mediarecorder", e);

            return false;
        }

        // create virtual display
        if (mMediaProjection != null) {
            try {
                mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                        "ZidoScreenCast",
                        DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mMediaRecorder.getSurface(), null, null);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "Failed to init virtual display", e);

                teardown();

                return false;
            }
        } else {
            teardown();

            return false;
        }

        try {
            mMediaRecorder.start();
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "Failed to start mediarecorder", e);

            teardown();

            return false;
        }

        startInForeground();

        return true;
    }

    public void stop() {
        stopForeground(true);

        teardown();
    }

    public boolean isRunning() {
        return ffwrapper.isProcessRunning();
    }

    private void startInForeground() {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Screen Cast")
                .setContentText("Screen cast is in progress")
                .setContentIntent(pendingIntent).build();

        startForeground(StreamService.class.getName().hashCode(), notification);
    }

    private void teardown(){
        ffwrapper.stop();
        releaseMediaRecorder();
        releaseVirtualDisplay();
        destroyMediaProjection();
    }

    public void onDestroy() {
        teardown();

        super.onDestroy();
    }


    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }

    private void releaseVirtualDisplay() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            //mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

}
