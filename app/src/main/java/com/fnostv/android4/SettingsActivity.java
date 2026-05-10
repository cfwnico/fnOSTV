package com.fnostv.android4;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.fnostv.android4.config.ProfileStore;
import com.fnostv.android4.config.ProfileValidation;
import com.fnostv.android4.config.ProfileValidator;
import com.fnostv.android4.config.ServerProfile;
import com.fnostv.android4.ui.SettingsForm;

public final class SettingsActivity extends Activity implements SettingsForm.Listener {
    private ProfileStore store;
    private SettingsForm settingsForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        store = new ProfileStore(this);
        settingsForm = new SettingsForm(this, this);
        setContentView(settingsForm.create(store.load()));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onCancelRequested();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveRequested(ServerProfile profile) {
        ProfileValidation validation = ProfileValidator.validate(profile);
        if (!validation.isValid()) {
            Toast.makeText(this, validation.getMessage(), Toast.LENGTH_SHORT).show();
            settingsForm.focusServerUrl();
            return;
        }
        store.save(profile);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onCancelRequested() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
