package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.Contact;
import com.example.roomatch.model.repository.UserRepository;

import java.util.List;

public class MatchRequestsAdapter extends RecyclerView.Adapter<MatchRequestsAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onApprove(String requestId);
        void onReject(String requestId);
    }



    public interface OnItemClickListener {
        void onItemClick(Contact contact, int position);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.itemClickListener = l;
    }

    private List<Contact> requests;

    private final OnRequestActionListener listener;

    public MatchRequestsAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        if (requests != null && position < requests.size()) {
            Contact request = requests.get(position);
            holder.nameTextView.setText(request.getFullName());
            holder.statusTextView.setText("סטטוס: " + (request.getStatus() != null ? request.getStatus() : "לא ידוע"));
            holder.approveButton.setOnClickListener(v -> listener.onApprove(request.getUserId()));
            holder.rejectButton.setOnClickListener(v -> listener.onReject(request.getUserId()));
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && itemClickListener != null) {
                    itemClickListener.onItemClick(requests.get(pos), pos); // בטוח מול ריסייקל
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public void setRequests(List<Contact> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }



    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView statusTextView; // הוספה לתמיכה ב-status
        ImageButton approveButton;
        ImageButton rejectButton;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.requestNameTextView);
            statusTextView = itemView.findViewById(R.id.requestStatusTextView); // ID חדש
            approveButton =(ImageButton) itemView.findViewById(R.id.approveButton);
            rejectButton = (ImageButton) itemView.findViewById(R.id.rejectButton);
        }


    }
}