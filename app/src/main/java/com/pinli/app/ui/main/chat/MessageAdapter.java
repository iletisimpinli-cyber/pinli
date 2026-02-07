// File: app/src/main/java/com/pinli/app/ui/main/chat/MessageAdapter.java
package com.pinli.app.ui.main.chat;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pinli.app.data.model.Message;
import com.pinli.app.databinding.ItemMessageInBinding;
import com.pinli.app.databinding.ItemMessageOutBinding;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_IN = 1;
    private static final int TYPE_OUT = 2;

    private final ChatViewModel vm;
    private final List<Message> items = new ArrayList<>();

    public MessageAdapter(@NonNull ChatViewModel vm) {
        this.vm = vm;
    }

    public void submit(List<Message> msgs) {
        items.clear();
        if (msgs != null) items.addAll(msgs);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message m = items.get(position);
        return vm.isMine(m) ? TYPE_OUT : TYPE_IN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_OUT) {
            ItemMessageOutBinding vb = ItemMessageOutBinding.inflate(inf, parent, false);
            return new VHOut(vb);
        } else {
            ItemMessageInBinding vb = ItemMessageInBinding.inflate(inf, parent, false);
            return new VHIn(vb);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = items.get(position);
        String text = (m != null && m.text != null) ? m.text : "";

        if (holder instanceof VHOut) {
            ((VHOut) holder).vb.tvText.setText(text);
        } else if (holder instanceof VHIn) {
            ((VHIn) holder).vb.tvText.setText(text);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VHIn extends RecyclerView.ViewHolder {
        final ItemMessageInBinding vb;
        VHIn(ItemMessageInBinding vb) { super(vb.getRoot()); this.vb = vb; }
    }

    static class VHOut extends RecyclerView.ViewHolder {
        final ItemMessageOutBinding vb;
        VHOut(ItemMessageOutBinding vb) { super(vb.getRoot()); this.vb = vb; }
    }
}
