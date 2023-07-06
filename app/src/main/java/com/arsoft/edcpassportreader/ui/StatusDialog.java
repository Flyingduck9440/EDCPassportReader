package com.arsoft.edcpassportreader.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.arsoft.edcpassportreader.databinding.StatusDialogBinding;

import java.util.Objects;

public class StatusDialog extends DialogFragment {

    StatusDialogBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = StatusDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.statusDialogClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Objects.requireNonNull(getDialog()).dismiss();
            }
        });
    }

    public void Update(String status, String message) {
        if (status != null) {
            binding.statusDialogStatus.setText(status);
            if (status.equals("Read Completed")) {
                binding.statusDialogStatus.setTextColor(Color.GREEN);
                binding.statusDialogProgress.setVisibility(View.GONE);
            }
        }
        if (message != null) {
            binding.statusDialogLog.append(message + "\n");
        }
    }
}
