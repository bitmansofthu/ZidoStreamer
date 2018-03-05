package eu.danman.zidostreamer.zidostreamer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

    private static final int REQUEST_CODE = 1000;

    private boolean streamServiceBound = false;
    private StreamService streamService = null;

    public static class StreamActiveDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Streaming is active")
                    .setMessage("You can stop streaming with stop stream button.")
                    .setCancelable(true)
                    .setNegativeButton("Stop Stream", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((SettingsActivity)getActivity()).streamService.stop();
                        }
                    })
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().finish();
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        private FFmpegWrapper ffwrapper;

        private boolean serviceHasPermission = false;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            setHasOptionsMenu(true);

            // TODO check if stream is running
            ffwrapper = new FFmpegWrapper(getActivity());

            init();

            if (ffwrapper.checkFFmpeg()) {
                findPreference("ffmpeg_tester").setTitle("FFmpeg info");
                ffwrapper.version(new FFmpegWrapper.ProcessListener() {
                    @Override
                    public void onProcessFinished(FFmpegWrapper wrapper) {
                        String log = wrapper.getOutputLog();
                        findPreference("ffmpeg_tester").setSummary(log);
                    }
                });
            }

        }

        @Override
        public void onStart() {
            super.onStart();

        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.settings_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_startstop:
                    startStream();
                    break;
            }

            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode != REQUEST_CODE) {
                Log.e(getClass().getSimpleName(), "Unknown request code: " + requestCode);
                return;
            }

            if (resultCode != RESULT_OK) {
                Toast.makeText(getActivity(),
                        "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();

                return;
            }

            serviceHasPermission = true;

            // configure and start the stream
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

            ((SettingsActivity)getActivity()).streamService.configure(resultCode, data, metrics.densityDpi);
            boolean started = ((SettingsActivity)getActivity()).streamService.start();
        }

        private void init() {
            final EditTextPreference streamurl = (EditTextPreference) findPreference("stream_url");
            final EditTextPreference ffmpegcmd = (EditTextPreference) findPreference("ffmpeg_cmd");

            streamurl.setSummary(streamurl.getText());

            streamurl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    streamurl.setSummary((String)newValue);

                    return true;
                }
            });
        }

        private void startStream() {
            if (!serviceHasPermission) {
                MediaProjectionManager manager = (MediaProjectionManager) getActivity().getSystemService
                        (Context.MEDIA_PROJECTION_SERVICE);

                startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE);

                return;
            }

            boolean started = ((SettingsActivity)getActivity()).streamService.start();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, StreamService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(mConnection);
        streamServiceBound = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            StreamService.LocalBinder binder = (StreamService.LocalBinder) service;
            streamService = binder.getService();
            streamServiceBound = true;

            if (streamService.isRunning()) {
                StreamActiveDialog dialog = new StreamActiveDialog();
                dialog.show(getFragmentManager().beginTransaction(), "streamactivedialog");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            streamServiceBound = false;
        }
    };
}