package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class PartnerAdapter extends RecyclerView.Adapter<PartnerAdapter.PartnerViewHolder> {

    @FunctionalInterface
    public interface OnProfileClickListener {
        void onProfileClick(UserProfile partner);
    }

    @FunctionalInterface
    public interface OnReportClickListener {
        void onReportClick(UserProfile partner);
    }

    private List<UserProfile> partnerList;
    private final OnProfileClickListener profileListener;
    private final OnReportClickListener reportListener;

    public PartnerAdapter(List<UserProfile> partnerList,
                          OnProfileClickListener profileListener,
                          OnReportClickListener reportListener) {
        this.partnerList = partnerList != null ? new ArrayList<>(partnerList) : new ArrayList<>();
        this.profileListener = profileListener;
        this.reportListener = reportListener;
    }

    @NonNull
    @Override
    public PartnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_partner_card, parent, false);
        return new PartnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PartnerViewHolder holder, int position) {
        UserProfile partner = partnerList.get(position);

        String name = partner.getFullName() != null ? partner.getFullName() : "לא ידוע";
        String age = String.valueOf(partner.getAge() != 0 ? partner.getAge() : "לא צוין");
        String interests = partner.getInterests() != null ? partner.getInterests() : "לא צוין";
        String lifestyle = partner.getLifestyle() != null ? partner.getLifestyle() : "לא צוין";

        holder.textName.setText("שם: " + name);
        holder.textAge.setText("גיל: " + age);
        holder.textInterests.setText("תחומי עניין: " + interests);
        holder.textLifestyle.setText("סגנון חיים: " + lifestyle);

        holder.buttonInvite.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "הוזמן לקבוצה!", Toast.LENGTH_SHORT).show());

        holder.buttonMessage.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "הודעה נשלחה! (כאילו)", Toast.LENGTH_SHORT).show());

        holder.buttonViewProfile.setOnClickListener(v -> profileListener.onProfileClick(partner));

        holder.buttonReport.setOnClickListener(v -> reportListener.onReportClick(partner));
    }

    @Override
    public int getItemCount() {
        return partnerList.size();
    }

    public void updatePartners(List<UserProfile> newPartners) {
        partnerList.clear();
        if (newPartners != null) {
            partnerList.addAll(newPartners);
        }
        notifyDataSetChanged();
    }

    static class PartnerViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textAge, textInterests, textLifestyle;
        Button buttonInvite, buttonMessage;
        TextView buttonViewProfile, buttonReport;

        public PartnerViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textAge = itemView.findViewById(R.id.textAge);
            textInterests = itemView.findViewById(R.id.textInterests);
            textLifestyle = itemView.findViewById(R.id.textLifestyle);
            buttonInvite = itemView.findViewById(R.id.buttonInvite);
            buttonMessage = itemView.findViewById(R.id.buttonMessage);
            buttonViewProfile = itemView.findViewById(R.id.buttonViewProfile);
            buttonReport = itemView.findViewById(R.id.buttonReport);
        }
    }
}