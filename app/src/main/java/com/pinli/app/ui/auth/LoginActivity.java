// File: app/src/main/java/com/pinli/app/ui/auth/LoginActivity.java
package com.pinli.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.pinli.app.R;
import com.pinli.app.data.repo.AuthRepository;
import com.pinli.app.databinding.ActivityLoginBinding;
import com.pinli.app.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding vb;
    private LoginViewModel vm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(vb.getRoot());

        vm = new ViewModelProvider(this).get(LoginViewModel.class);

        if (vm.isSignedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (vb.codeGroup.getVisibility() == View.VISIBLE) {
                    vb.codeGroup.setVisibility(View.GONE);
                    vb.phoneGroup.setVisibility(View.VISIBLE);
                } else {
                    finish();
                }
            }
        });

        vm.loading().observe(this, isLoading -> {
            boolean l = Boolean.TRUE.equals(isLoading);
            vb.progress.setVisibility(l ? View.VISIBLE : View.GONE);
            vb.btnSendSms.setEnabled(!l);
            vb.btnVerify.setEnabled(!l);
        });

        vm.toast().observe(this, e -> Snackbar.make(vb.getRoot(), e.message, Snackbar.LENGTH_LONG).show());

        vm.verificationId().observe(this, vid -> {
            if (vid != null) {
                vb.phoneGroup.setVisibility(View.GONE);
                vb.codeGroup.setVisibility(View.VISIBLE);
                vb.etCode.requestFocus();
            }
        });

        vm.goMain().observe(this, go -> {
            if (Boolean.TRUE.equals(go)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });

        vb.btnSendSms.setOnClickListener(v -> {
            String phone = vb.etPhone.getText() != null ? vb.etPhone.getText().toString().trim() : "";
            if (TextUtils.isEmpty(phone) || !phone.startsWith("+") || phone.length() < 10) {
                vm.showError(getString(R.string.err_phone_format));
                return;
            }
            vm.setLoading(true);
            vm.authRepo().sendSms(this, phone, new AuthRepository.CodeSentCallback() {
                @Override public void onCodeSent(@androidx.annotation.NonNull String verificationId) {
                    vm.setLoading(false);
                    vm.onSmsSent(verificationId);
                }

                @Override public void onError(@androidx.annotation.NonNull String message) {
                    vm.showError(message);
                }
            });
        });

        vb.btnVerify.setOnClickListener(v -> {
            String vid = vm.verificationId().getValue();
            String code = vb.etCode.getText() != null ? vb.etCode.getText().toString().trim() : "";
            if (vid == null || "AUTO".equals(vid)) {
                vm.showError(getString(R.string.err_wait_auto));
                return;
            }
            if (code.length() < 4) {
                vm.showError(getString(R.string.err_code));
                return;
            }
            vm.setLoading(true);
            vm.authRepo().verifyCode(vid, code, new AuthRepository.SignInCallback() {
                @Override public void onSignedIn(@androidx.annotation.NonNull String uid) {
                    vm.onSignedInEnsureUser(uid);
                }

                @Override public void onError(@androidx.annotation.NonNull String message) {
                    vm.showError(message);
                }
            });
        });
    }
}
