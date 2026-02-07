// File: app/src/main/java/com/pinli/app/ui/main/MainActivity.java
package com.pinli.app.ui.main;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.pinli.app.R;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.databinding.ActivityMainBinding;
import com.pinli.app.ui.auth.LoginActivity;
import com.pinli.app.ui.main.chat.ChatsFragment;
import com.pinli.app.ui.main.map.MapFragment;
import com.pinli.app.ui.main.profile.ProfileFragment;
import com.pinli.app.ui.main.status.StatusFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding vb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(vb.getRoot());

        vb.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                setFragment(new MapFragment());
                return true;
            } else if (id == R.id.nav_status) {
                setFragment(new StatusFragment());
                return true;
            } else if (id == R.id.nav_chats) {
                setFragment(new ChatsFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                setFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            vb.bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auth gate: if user is not signed in, kick to LoginActivity.
        if (FirebaseRefs.auth().getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setFragment(Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, f)
                .commit();
    }

    public void showMessage(String msg) {
        Snackbar.make(vb.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }
}
