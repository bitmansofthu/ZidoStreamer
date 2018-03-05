package eu.danman.zidostreamer.zidostreamer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {

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
                            // TODO stop stream
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
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.settings_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_startstop:
                    //startActivityForResult();
                    break;
            }

            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {

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
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

        // TODO show streaming dialog if streaming is running
        //StreamActiveDialog dialog = new StreamActiveDialog();
        //dialog.show(getFragmentManager().beginTransaction(), "streamactivedialog");
    }


}