package com.example.roomatch.adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;
import java.util.List;
public class PartnerAdapter extends
        RecyclerView.Adapter<PartnerAdapter.PartnerViewHolder> {
    private final List<UserProfile> partnerList;
    private final OnProfileClickListener profileClickListener;
    private final OnMatchRequestClickListener matchClickListener;
    private final OnReportClickListener reportClickListener;
    public PartnerAdapter(List<UserProfile> partnerList,
                          OnProfileClickListener profileClickListener,
                          OnMatchRequestClickListener matchClickListener,
                          OnReportClickListener reportClickListener) {this.partnerList = partnerList;
        this.profileClickListener = profileClickListener;
        this.matchClickListener = matchClickListener;
        this.reportClickListener = reportClickListener;
    }
    public interface OnProfileClickListener {
        void onProfileClick(UserProfile profile);
    }
    public interface OnMatchRequestClickListener {
        void onMatchRequest(UserProfile profile);
    }
    public interface OnReportClickListener {
        void onReport(UserProfile profile);
    }
    @NonNull
    @Override
    public PartnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int
            viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_partner_card, parent, false);
        return new PartnerViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(@NonNull PartnerViewHolder holder, int position) {
        UserProfile partner = partnerList.get(position);
        holder.bind(partner);
    }
    @Override
    public int getItemCount() {
        return partnerList.size();
    }
    public void setData(List<UserProfile> newList) {
        partnerList.clear();
        partnerList.addAll(newList);
        notifyDataSetChanged();
    }
    class PartnerViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView lifestyleText;
        private final Button btnView;
        private final Button btnMatch;
        private final Button btnReport;
        public PartnerViewHolder(@NonNull View itemView) {super(itemView);
            nameText = itemView.findViewById(R.id.partnerNameTextView);
            lifestyleText = itemView.findViewById(R.id.partnerLifestyleTextView);
            btnView = itemView.findViewById(R.id.buttonViewProfile);
            btnMatch = itemView.findViewById(R.id.buttonRequestMatch);
            btnReport = itemView.findViewById(R.id.buttonReport);
        }
        public void bind(UserProfile profile) {
            nameText.setText(profile.getFullName());
            lifestyleText.setText("×¡×’× ×•×Ÿ ×—×™×™× : " + profile.getLifestyle());
            btnView.setOnClickListener(v -> profileClickListener.onProfileClick(profile));
            btnMatch.setOnClickListener(v -> matchClickListener.onMatchRequest(profile));
            btnReport.setOnClickListener(v -> reportClickListener.onReport(profile));
        }
    }
}