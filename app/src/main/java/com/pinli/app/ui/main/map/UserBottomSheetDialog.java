// File: app/src/main/java/com/pinli/app/ui/main/map/UserBottomSheetDialog.java
package com.pinli.app.ui.main.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pinli.app.R;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.User;
import com.pinli.app.data.repo.BlockRepository;
import com.pinli.app.data.repo.ChatRepository;
import com.pinli.app.data.repo.FollowRepository;
import com.pinli.app.data.repo.ReportRepository;
import com.pinli.app.data.repo.UserRepository;
import com.pinli.app.databinding.BottomsheetUserBinding;
import com.pinli.app.ui.main.chat.ChatActivity;

public class UserBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_UID = "uid";

    private BottomsheetUserBinding vb;

    private final UserRepository userRepo = new UserRepository();
    private final FollowRepository followRepo = new FollowRepository();
    private final BlockRepository blockRepo = new BlockRepository();
    private final ChatRepository chatRepo = new ChatRepository();
    private final ReportRepository reportRepo = new ReportRepository();

    private String otherUid;
    private User otherUser;
    private FollowRepository.FollowState followState = new FollowRepository.FollowState();
    private BlockRepository.BlockState blockState = new BlockRepository.BlockState();

    public static UserBottomSheetDialog newInstance(@NonNull String uid) {
        UserBottomSheetDialog d = new UserBottomSheetDialog();
        Bundle b = new Bundle();
        b.putString(ARG_UID, uid);
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vb = BottomsheetUserBinding.inflate(inflater, container, false);
        return vb.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        otherUid = getArguments() != null ? getArguments().getString(ARG_UID) : null;

        vb.btnFollow.setEnabled(false);
        vb.btnMessage.setEnabled(false);
        vb.btnBlock.setEnabled(false);
        vb.btnReport.setEnabled(false);

        vb.btnFollow.setOnClickListener(v -> onFollowClicked());
        vb.btnMessage.setOnClickListener(v -> onMessageClicked());
        vb.btnBlock.setOnClickListener(v -> onBlockClicked());
        vb.btnReport.setOnClickListener(v -> onReportClicked());

        load();
    }

    private void load() {
        String meUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (meUid == null || otherUid == null) {
            dismissAllowingStateLoss();
            return;
        }
        if (meUid.equals(otherUid)) {
            vb.tvName.setText(getString(R.string.user_sheet_you));
            vb.tvMeta.setText(getString(R.string.user_sheet_meta_you));
            vb.btnFollow.setEnabled(false);
            vb.btnMessage.setEnabled(false);
            vb.btnBlock.setEnabled(false);
            vb.btnReport.setEnabled(false);
            return;
        }

        vb.progress.setVisibility(View.VISIBLE);

        userRepo.getUser(otherUid, new UserRepository.Callback<User>() {
            @Override public void onSuccess(@NonNull User data) {
                otherUser = data;
                vb.tvName.setText(data.displayName != null ? data.displayName : getString(R.string.user_sheet_unknown));
                vb.tvMeta.setText(data.isPrivate ? getString(R.string.user_sheet_meta_private) : getString(R.string.user_sheet_meta_public));
                loadStates(meUid);
            }

            @Override public void onError(@NonNull String message) {
                vb.progress.setVisibility(View.GONE);
                vb.tvName.setText(getString(R.string.user_sheet_unknown));
                vb.tvMeta.setText(message);
            }
        });
    }

    private void loadStates(@NonNull String meUid) {
        followRepo.getFollowState(meUid, otherUid, new FollowRepository.Callback<FollowRepository.FollowState>() {
            @Override public void onSuccess(@NonNull FollowRepository.FollowState data) {
                followState = data;
                blockRepo.getBlockState(meUid, otherUid, new BlockRepository.Callback<BlockRepository.BlockState>() {
                    @Override public void onSuccess(@NonNull BlockRepository.BlockState data2) {
                        blockState = data2;
                        vb.progress.setVisibility(View.GONE);
                        updateButtons();
                    }

                    @Override public void onError(@NonNull String message) {
                        vb.progress.setVisibility(View.GONE);
                        toast(message);
                        updateButtons();
                    }
                });
            }

            @Override public void onError(@NonNull String message) {
                vb.progress.setVisibility(View.GONE);
                toast(message);
                updateButtons();
            }
        });
    }

    private void updateButtons() {
        // If either direction blocked, disable follow/message.
        boolean blockedAny = blockState.iBlocked || blockState.blockedMe;
        if (blockedAny) {
            vb.tvMeta.setText(blockState.blockedMe ? getString(R.string.user_sheet_blocked_me) : getString(R.string.user_sheet_blocked));
        }

        vb.btnFollow.setEnabled(!blockedAny);
        vb.btnMessage.setEnabled(!blockedAny && followState.canMessage());
        vb.btnBlock.setEnabled(true);
        vb.btnReport.setEnabled(true);

        // Follow button text
        if (followState.iFollow) {
            vb.btnFollow.setText(getString(R.string.unfollow));
        } else if (followState.outgoingRequestPending) {
            vb.btnFollow.setText(getString(R.string.requested));
        } else if (otherUser != null && otherUser.isPrivate) {
            vb.btnFollow.setText(getString(R.string.request_follow));
        } else {
            vb.btnFollow.setText(getString(R.string.follow));
        }

        vb.btnMessage.setText(followState.canMessage() ? getString(R.string.message) : getString(R.string.message_locked));
        vb.btnBlock.setText(blockState.iBlocked ? getString(R.string.unblock) : getString(R.string.block));
    }

    private void onFollowClicked() {
        String meUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (meUid == null || otherUid == null) return;
        if (otherUser == null) return;

        vb.progress.setVisibility(View.VISIBLE);
        vb.btnFollow.setEnabled(false);

        if (followState.iFollow) {
            followRepo.unfollow(meUid, otherUid, new FollowRepository.Callback<Boolean>() {
                @Override public void onSuccess(@NonNull Boolean data) { reloadQuick(meUid); }
                @Override public void onError(@NonNull String message) { vb.progress.setVisibility(View.GONE); toast(message); vb.btnFollow.setEnabled(true); }
            });
            return;
        }

        if (followState.outgoingRequestPending) {
            // cancel request
            followRepo.cancelRequest(meUid, otherUid, new FollowRepository.Callback<Boolean>() {
                @Override public void onSuccess(@NonNull Boolean data) { reloadQuick(meUid); }
                @Override public void onError(@NonNull String message) { vb.progress.setVisibility(View.GONE); toast(message); vb.btnFollow.setEnabled(true); }
            });
            return;
        }

        if (otherUser.isPrivate) {
            followRepo.requestFollowPrivate(meUid, otherUid, new FollowRepository.Callback<Boolean>() {
                @Override public void onSuccess(@NonNull Boolean data) { reloadQuick(meUid); }
                @Override public void onError(@NonNull String message) { vb.progress.setVisibility(View.GONE); toast(message); vb.btnFollow.setEnabled(true); }
            });
        } else {
            followRepo.followPublic(meUid, otherUid, new FollowRepository.Callback<Boolean>() {
                @Override public void onSuccess(@NonNull Boolean data) { reloadQuick(meUid); }
                @Override public void onError(@NonNull String message) { vb.progress.setVisibility(View.GONE); toast(message); vb.btnFollow.setEnabled(true); }
            });
        }
    }

    private void onMessageClicked() {
        String meUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (meUid == null || otherUid == null) return;

        if (!followState.canMessage()) {
            toast(getString(R.string.err_message_requires_mutual));
            return;
        }
        if (blockState.iBlocked || blockState.blockedMe) {
            toast(getString(R.string.err_blocked));
            return;
        }

        vb.progress.setVisibility(View.VISIBLE);
        chatRepo.ensureChat(meUid, otherUid, new ChatRepository.Callback<String>() {
            @Override public void onSuccess(@NonNull String chatId) {
                vb.progress.setVisibility(View.GONE);
                Intent i = new Intent(requireContext(), ChatActivity.class);
                i.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId);
                i.putExtra(ChatActivity.EXTRA_OTHER_UID, otherUid);
                startActivity(i);
                dismissAllowingStateLoss();
            }

            @Override public void onError(@NonNull String message) {
                vb.progress.setVisibility(View.GONE);
                toast(message);
            }
        });
    }

    private void onBlockClicked() {
        String meUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (meUid == null || otherUid == null) return;

        if (blockState.iBlocked) {
            vb.progress.setVisibility(View.VISIBLE);
            blockRepo.unblock(meUid, otherUid, new BlockRepository.Callback<Boolean>() {
                @Override public void onSuccess(@NonNull Boolean data) {
                    vb.progress.setVisibility(View.GONE);
                    toast(getString(R.string.unblocked_done));
                    blockState.iBlocked = false;
                    updateButtons();
                }
                @Override public void onError(@NonNull String message) { vb.progress.setVisibility(View.GONE); toast(message); }
            });
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.block_confirm_title)
                .setMessage(R.string.block_confirm_message)
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.ok, (d, w) -> {
                    vb.progress.setVisibility(View.VISIBLE);
                    blockRepo.block(meUid, otherUid, new BlockRepository.Callback<Boolean>() {
                        @Override public void onSuccess(@NonNull Boolean data) {
                            vb.progress.setVisibility(View.GONE);
                            toast(getString(R.string.blocked_done));
                            blockState.iBlocked = true;
                            updateButtons();
                            dismissAllowingStateLoss();
                        }
                        @Override public void onError(@NonNull String message) { vb.progress.setVisibility(View.GONE); toast(message); }
                    });
                })
                .show();
    }

    private void onReportClicked() {
        String meUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (meUid == null || otherUid == null) return;

        String[] reasons = new String[] {
                getString(R.string.report_reason_spam),
                getString(R.string.report_reason_abuse),
                getString(R.string.report_reason_fake),
                getString(R.string.report_reason_other)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.report_title)
                .setItems(reasons, (dialog, which) -> {
                    String r = reasons[which];
                    vb.progress.setVisibility(View.VISIBLE);
                    reportRepo.reportUser(meUid, otherUid, r, new ReportRepository.Callback<Boolean>() {
                        @Override public void onSuccess(@NonNull Boolean data) {
                            vb.progress.setVisibility(View.GONE);
                            toast(getString(R.string.report_done));
                        }
                        @Override public void onError(@NonNull String message) {
                            vb.progress.setVisibility(View.GONE);
                            toast(message);
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    private void reloadQuick(@NonNull String meUid) {
        // Update local state quickly
        followRepo.getFollowState(meUid, otherUid, new FollowRepository.Callback<FollowRepository.FollowState>() {
            @Override public void onSuccess(@NonNull FollowRepository.FollowState data) {
                followState = data;
                vb.progress.setVisibility(View.GONE);
                vb.btnFollow.setEnabled(true);
                updateButtons();
            }
            @Override public void onError(@NonNull String message) {
                vb.progress.setVisibility(View.GONE);
                vb.btnFollow.setEnabled(true);
                toast(message);
            }
        });
    }

    private void toast(@NonNull String msg) {
        if (getContext() != null) android.widget.Toast.makeText(getContext(), msg, android.widget.Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        vb = null;
        super.onDestroyView();
    }
}
