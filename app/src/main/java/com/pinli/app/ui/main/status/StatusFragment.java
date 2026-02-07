// File: app/src/main/java/com/pinli/app/ui/main/status/StatusFragment.java
package com.pinli.app.ui.main.status;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.pinli.app.databinding.FragmentStatusBinding;

public class StatusFragment extends Fragment {

    private FragmentStatusBinding vb;
    private StatusViewModel vm;

    private final ActivityResultLauncher<Intent> pickImage =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
                if (res.getResultCode() == Activity.RESULT_OK && res.getData() != null) {
                    Uri uri = res.getData().getData();
                    if (uri != null) vm.setPhoto(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vb = FragmentStatusBinding.inflate(inflater, container, false);
        return vb.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(StatusViewModel.class);

        vm.loading().observe(getViewLifecycleOwner(), l ->
                vb.progress.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE)
        );
        vm.toast().observe(getViewLifecycleOwner(), e -> {
            if (getActivity() != null) {
                android.widget.Toast.makeText(getActivity(), e.message, android.widget.Toast.LENGTH_LONG).show();
            }
        });

        vb.btnEmoji.setOnClickListener(v -> {
            String emoji = vb.etEmoji.getText() != null ? vb.etEmoji.getText().toString().trim() : "";
            if (emoji.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), com.pinli.app.R.string.err_emoji, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            vm.setEmoji(emoji);
        });

        vb.btnPhoto.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            pickImage.launch(Intent.createChooser(i, getString(com.pinli.app.R.string.pick_photo)));
        });
    }

    @Override
    public void onDestroyView() {
        vb = null;
        super.onDestroyView();
    }
}
