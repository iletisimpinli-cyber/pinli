// File: app/src/main/java/com/pinli/app/ui/main/profile/ProfileFragment.java
package com.pinli.app.ui.main.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pinli.app.R;
import com.pinli.app.databinding.FragmentProfileBinding;
import com.pinli.app.ui.auth.LoginActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding vb;
    private ProfileViewModel vm;
    private boolean ignoreSwitch = false;
    private boolean ignorePrivate = false;
    private String originalDisplayName = "";
    private String originalBio = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vb = FragmentProfileBinding.inflate(inflater, container, false);
        return vb.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(ProfileViewModel.class);

        vm.start();

        vm.loading().observe(getViewLifecycleOwner(), l ->
                vb.progress.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE)
        );

        vm.toast().observe(getViewLifecycleOwner(), e -> {
            if (getActivity() != null) {
                android.widget.Toast.makeText(getActivity(), e.message, android.widget.Toast.LENGTH_LONG).show();
            }
        });

        vm.signedOut().observe(getViewLifecycleOwner(), out -> {
            if (Boolean.TRUE.equals(out) && getActivity() != null) {
                Intent i = new Intent(getActivity(), LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                getActivity().finish();
            }
        });

        vm.myUser().observe(getViewLifecycleOwner(), user -> {
            ignorePrivate = true;
            vb.switchPrivate.setChecked(user != null && user.isPrivate);
            ignorePrivate = false;
            if (user != null && user.displayName != null) {
                originalDisplayName = user.displayName;
                vb.etDisplayName.setText(user.displayName);
            } else {
                originalDisplayName = "";
                vb.etDisplayName.setText("");
            }
            if (user != null && user.bio != null) {
                originalBio = user.bio;
                vb.etBio.setText(user.bio);
            } else {
                originalBio = "";
                vb.etBio.setText("");
            }
            updateSaveEnabled();
        });

        vm.incomingRequestCount().observe(getViewLifecycleOwner(), c -> {
            int count = c != null ? c : 0;
            String text = count > 0
                    ? getString(R.string.follow_requests_button_with_count, count)
                    : getString(R.string.follow_requests_button);
            vb.btnFollowRequests.setText(text);
        });

        vm.myLocation().observe(getViewLifecycleOwner(), ul -> {
            // Set initial state without triggering listener.
            ignoreSwitch = true;
            vb.switchHide24h.setChecked(ul != null && ul.isHidden);
            ignoreSwitch = false;

            boolean enabled = ul != null && ul.geohash != null && !ul.geohash.isEmpty();
            vb.switchHide24h.setEnabled(enabled);

            vb.tvHideInfo.setText(buildHideInfo(ul));
        });

        vb.switchPrivate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignorePrivate) return;
            vm.setPrivate(isChecked);
        });

        vb.btnFollowRequests.setOnClickListener(v -> {
            if (getActivity() == null) return;
            startActivity(new Intent(getActivity(), FollowRequestsActivity.class));
        });

        vb.etDisplayName.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                vb.tilDisplayName.setError(null);
                updateSaveEnabled();
            }
        });

        vb.etBio.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                updateSaveEnabled();
            }
        });

        vb.btnSaveProfile.setOnClickListener(v -> {
            String name = vb.etDisplayName.getText() != null ? vb.etDisplayName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                vb.tilDisplayName.setError(getString(R.string.err_display_name_empty));
                return;
            }
            String bio = vb.etBio.getText() != null ? vb.etBio.getText().toString().trim() : "";
            vm.updateProfile(name, bio, getString(R.string.profile_saved));
        });

        vb.switchHide24h.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignoreSwitch) return;
            if (!buttonView.isEnabled()) {
                ignoreSwitch = true;
                vb.switchHide24h.setChecked(false);
                ignoreSwitch = false;
                android.widget.Toast.makeText(requireContext(), getString(R.string.err_set_location_first), android.widget.Toast.LENGTH_LONG).show();
                return;
            }

            if (isChecked) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.hide_confirm_title)
                        .setMessage(R.string.hide_confirm_message)
                        .setNegativeButton(R.string.cancel, (d, w) -> {
                            d.dismiss();
                            ignoreSwitch = true;
                            vb.switchHide24h.setChecked(false);
                            ignoreSwitch = false;
                        })
                        .setPositiveButton(R.string.ok, (d, w) -> vm.toggleHidden24h(true))
                        .show();
            } else {
                vm.toggleHidden24h(false);
            }
        });

        vb.btnSignOut.setOnClickListener(v -> vm.signOut());
    }

    private void updateSaveEnabled() {
        String name = vb.etDisplayName.getText() != null ? vb.etDisplayName.getText().toString().trim() : "";
        String bio = vb.etBio.getText() != null ? vb.etBio.getText().toString().trim() : "";
        boolean changed = !name.equals(originalDisplayName) || !bio.equals(originalBio);
        vb.btnSaveProfile.setEnabled(changed && !name.isEmpty());
    }

    private String buildHideInfo(@Nullable com.pinli.app.data.model.UserLocation ul) {
        if (ul == null || ul.geohash == null || ul.geohash.isEmpty()) {
            return getString(R.string.hide_info_no_location);
        }
        if (ul.isHidden) {
            long now = System.currentTimeMillis();
            long left = Math.max(0L, ul.hiddenUntil - now);
            return getString(R.string.hide_info_hidden, formatRemaining(left));
        }
        return getString(R.string.hide_info_visible);
    }

    private String formatRemaining(long millis) {
        long totalMinutes = Math.max(0L, millis / 60000L);
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours <= 0) return minutes + "m";
        return hours + "h " + minutes + "m";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refreshIncomingRequestsCount();
    }

    @Override
    public void onDestroyView() {
        vb = null;
        super.onDestroyView();
    }
}
