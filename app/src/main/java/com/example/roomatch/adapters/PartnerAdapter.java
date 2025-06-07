package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;

import java.util.List;
import java.util.Map;

public class PartnerAdapter extends RecyclerView.Adapter<PartnerAdapter.PartnerViewHolder> {

    @FunctionalInterface
    public interface OnProfileClickListener {
        void onProfileClick(Map<String, Object> partner);
    }

    @FunctionalInterface
    public interface OnReportClickListener {
        void onReportClick(String fullName);
    }

    private final List<Map<String, Object>> partnerList;
    private final OnProfileClickListener profileListener;
    private final OnReportClickListener reportListener;

    public PartnerAdapter(List<Map<String, Object>> partnerList,
                          OnProfileClickListener profileListener,
                          OnReportClickListener reportListener) {
        this.partnerList = partnerList;
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
        Map<String, Object> partner = partnerList.get(position);

        String name = safeToString(partner.get("fullName"), "לא ידוע");
        String age = safeToString(partner.get("age"), "לא צוין");
        String interests = safeToString(partner.get("interests"), "לא צוין");
        String lifestyle = safeToString(partner.get("lifestyle"), "לא צוין");

        holder.textName.setText("שם: " + name);
        holder.textAge.setText("גיל: " + age);
        holder.textInterests.setText("תחומי עניין: " + interests);
        holder.textLifestyle.setText("סגנון חיים: " + lifestyle);

        holder.buttonInvite.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "הוזמן לקבוצה!", Toast.LENGTH_SHORT).show());

        holder.buttonMessage.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "הודעה נשלחה! (כאילו)", Toast.LENGTH_SHORT).show());

        holder.buttonViewProfile.setOnClickListener(v -> profileListener.onProfileClick(partner));

        holder.buttonReport.setOnClickListener(v -> reportListener.onReportClick(name));
    }

    @Override
    public int getItemCount() {
        return partnerList.size();
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

    private String safeToString(Object value, String defaultValue) {
        return (value != null) ? value.toString() : defaultValue;
    }
}
