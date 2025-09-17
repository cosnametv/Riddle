package com.cosname.infiniteriddle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChangeUsernameBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_change_username, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText input = view.findViewById(R.id.newUsernameInput);
        MaterialButton save = view.findViewById(R.id.saveButton);
        MaterialButton cancel = view.findViewById(R.id.cancelButton);
        TextView current = view.findViewById(R.id.currentUsername);

        SharedPreferences appPrefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String currentUsername = appPrefs.getString("username", "");
        current.setText("Current: " + currentUsername);
        input.setText(currentUsername);

        save.setOnClickListener(v -> {
            String newUsername = input.getText() != null ? input.getText().toString().trim() : "";
            if (newUsername.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newUsername.length() < 5) {
                Toast.makeText(requireContext(), "Username must be at least 5 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newUsername.length() > 20) {
                Toast.makeText(requireContext(), "Username must be 20 characters or less", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newUsername.contains(".")) {
                Toast.makeText(requireContext(), "Usernames cannot contain dots.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!((RiddleActivity) requireActivity()).isOnline()) {
                Toast.makeText(requireContext(), "No internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }

            appPrefs.edit().putString("username", newUsername).apply();
            ((RiddleActivity) requireActivity()).updateUsernameInFirebase(newUsername);
            Toast.makeText(requireContext(), "Username updated!", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        cancel.setOnClickListener(v -> dismiss());
    }
}


