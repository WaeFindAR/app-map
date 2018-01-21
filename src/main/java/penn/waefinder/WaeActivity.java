package penn.waefinder;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class WaeActivity extends AppCompatActivity {
    private EditText mAddressView;
    private MediaPlayer mMediaPlayer;
    private Geocoder geo = new Geocoder(this);;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wae);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer = MediaPlayer.create(this, R.raw.dounodawae);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(false);
        mMediaPlayer.start();

        mAddressView = findViewById(R.id.address);
        mAddressView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }

    protected void attemptLogin() {
        if (mAddressView.length() != 0) {
            if (geo.isPresent()) {
                String content = mAddressView.getText().toString();
                Log.println(Log.INFO, "MYDEBUGGERTHINGY", content);
                List<Address> locations = null;
                try {
                    locations = geo.getFromLocationName(content, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (locations != null && !locations.isEmpty()) {
                    Address local = locations.get(0);
                    String res = String.format("%s, %s", local.getLatitude(), local.getLongitude());
                    Log.println(Log.INFO, "MYDEBUGGERTHINGY", res);
                    mMediaPlayer = MediaPlayer.create(this, R.raw.thisisdawae);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setLooping(false);
                    mMediaPlayer.start();

                    Intent intent = new Intent(this, MapsActivity.class);
                    intent.putExtra("LATITUDE", local.getLatitude());
                    intent.putExtra("LONGITUDE", local.getLongitude());
                    startActivity(intent);
                } else {
                    mMediaPlayer = MediaPlayer.create(this, R.raw.udonotnodawae);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setLooping(false);
                    mMediaPlayer.start();

                    Toast.makeText(this, "Booda, I do not see the way", Toast.LENGTH_SHORT).show();
                    Log.println(Log.INFO, "MYDEBUGGERTHINGY", "No Results");
                }
            }
        } else {
            mMediaPlayer = MediaPlayer.create(this, R.raw.udonotnodawae);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setLooping(false);
            mMediaPlayer.start();
            Toast.makeText(this, "Booda, Tell me the way", Toast.LENGTH_SHORT).show();
        }
    }

}
