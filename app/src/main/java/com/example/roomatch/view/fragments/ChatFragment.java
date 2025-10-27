package com.example.roomatch.view.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomatch.R;
import com.example.roomatch.adapters.ChatAdapter;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private ChatViewModel viewModel;
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private Button sendButton, backButton;
    private ChatAdapter adapter;

    // מגיעים מה-arguments
    private String chatId;        // לצ'אט פרטי = chatId; לקבוצתי = groupChatId
    private String otherUserId;   // לפרטי בלבד
    private String apartmentId;   // אופציונלי
    private boolean isGroup = false;

    private LinearLayoutManager layoutManager;
    private RecyclerView.OnScrollListener pagingScrollListener;

    public ChatFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> c) {
                return (T) new ChatViewModel(
                        new UserRepository(),
                        new ChatRepository(requireContext()),
                        new ApartmentRepository()
                );
            }
        }).get(ChatViewModel.class);

        // ---- קבלת פרמטרים מה־arguments ----
        Bundle args = getArguments();
        if (args != null) {
            chatId      = args.getString("chatId");
            otherUserId = args.getString("otherUserId");
            apartmentId = args.getString("apartmentId");
            isGroup     = args.getBoolean("isGroup", false);
        }

        String currentUid = viewModel.getCurrentUserId();
        if (currentUid == null) {
            Toast.makeText(getContext(), "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        // ⚠️ fallback ל-chatId מותר רק בצ'אט פרטי
        if ((chatId == null || chatId.trim().isEmpty())) {
            if (!isGroup) {
                if (otherUserId != null && apartmentId != null) {
                    chatId = generateConsistentChatId(currentUid, otherUserId, apartmentId);
                    Log.d(TAG, "chatId was missing; generated fallback chatId=" + chatId);
                } else {
                    Toast.makeText(getContext(), "חסרים נתונים לפתיחת צ'אט", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
            } else {
                // בקבוצתי תמיד חייב להגיע groupChatId אמיתי מהרשימה
                Toast.makeText(getContext(), "חסר מזהה צ'אט קבוצתי", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }
        }

        // ---- UI ----
        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        messageEditText = view.findViewById(R.id.editTextMessage);
        sendButton = view.findViewById(R.id.buttonSend);
        backButton = view.findViewById(R.id.buttonBack);

        adapter = new ChatAdapter(new ArrayList<>(), currentUid);

        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true); // להשאיר כפי שהיה
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // ---- Observe ----
        viewModel.getMessages().observe(getViewLifecycleOwner(), list -> adapter.updateMessages(list));
        viewModel.getToast().observe(getViewLifecycleOwner(),
                msg -> { if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show(); });

        // ---- פגינציית הודעות ----
        pagingScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy >= 0) return;

                int visible = layoutManager.getChildCount();
                int total = layoutManager.getItemCount();
                int first = layoutManager.findFirstVisibleItemPosition();

                if (!viewModel.isLoading() && viewModel.hasMore()
                        && (first + visible) >= (total - 5)) {
                    viewModel.loadNextPage(chatId, isGroup);
                }
            }
        };
        recyclerView.addOnScrollListener(pagingScrollListener);

        // ---- עמוד ראשון של הודעות ----
        viewModel.loadFirstPage(chatId, isGroup);

        // ---- שליחה ----
        sendButton.setOnClickListener(v -> {
            String text = messageEditText.getText().toString().trim();
            if (text.isEmpty()) return;

            if (isGroup) {
                viewModel.sendGroupMessage(chatId, text); // chatId == groupChatId
            } else {
                if (otherUserId == null || otherUserId.trim().isEmpty()) {
                    Toast.makeText(getContext(), "לא ניתן לשלוח: לא זוהה נמען הצ'אט", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.sendMessage(chatId, otherUserId, apartmentId, text);
            }
            messageEditText.setText("");
        });

        // ---- חזרה ----
        backButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // ---- סימון כהודעות נקראו ----
        // כרגע יש מימוש לסימון בפרטי בלבד
        if (!isGroup) {
            viewModel.markMessagesAsRead(chatId);
        }
        // אם תרצה גם לקבוצות: הוסף VM API שעוטף chatRepo.markGroupMessagesAsRead(...)
    }

    /** fallback בלבד לצ'אט פרטי: יוצר chatId עקבי ע"י מיון userIds ואז הוספת apartmentId. */
    private String generateConsistentChatId(String userId1, String userId2, String apartmentId) {
        if (userId1 == null || userId2 == null || apartmentId == null) {
            throw new IllegalArgumentException("User IDs and apartment ID must not be null");
        }
        List<String> ids = Arrays.asList(userId1, userId2);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1) + "_" + apartmentId;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // אין since המאזינים קשורים ל-lifecycle
    }
}
