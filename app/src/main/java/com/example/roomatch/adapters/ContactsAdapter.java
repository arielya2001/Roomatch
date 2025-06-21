package com.example.roomatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.model.Contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    public interface OnContactSelectionListener {
        void onCreateGroup(List<String> selectedUserIds);
    }

    private List<Contact> contacts = new ArrayList<>();
    private final OnContactSelectionListener listener;
    private final Map<Integer, Boolean> selectedContacts = new HashMap<>();

    public ContactsAdapter(OnContactSelectionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.nameTextView.setText(contact.getFullName());
        holder.checkBox.setChecked(selectedContacts.getOrDefault(position, false));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedContacts.put(position, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts.clear();
        this.contacts.addAll(contacts);
        selectedContacts.clear();
        notifyDataSetChanged();
    }

    public List<String> getSelectedContacts() {
        List<String> selectedIds = new ArrayList<>();
        for (int i = 0; i < contacts.size(); i++) {
            if (selectedContacts.getOrDefault(i, false)) {
                selectedIds.add(contacts.get(i).getUserId());
            }
        }
        return selectedIds;
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        CheckBox checkBox;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contactNameTextView);
            checkBox = itemView.findViewById(R.id.contactCheckBox);
        }
    }
}