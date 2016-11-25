package at.str.evan.wakemecgm;

import android.app.Application;

/**
 * Created by Evan on 11/25/2016.
 */

public class GlobalVars extends Application {
    private boolean playingAlertTone = false;

    public void setPlayingAlertTone(boolean val) {
        playingAlertTone = val;
    }

    public boolean getPlayingAlertTone() {
        return playingAlertTone;
    }
}
