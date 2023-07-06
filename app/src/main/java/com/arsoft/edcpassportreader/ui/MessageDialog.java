package com.arsoft.edcpassportreader.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MessageDialog extends DialogFragment {

    public interface MessageDialogListener {
        void onPositiveClick(DialogFragment dialog);
        void onNegativeClick(DialogFragment dialog);
    }

    MessageDialogListener listener;
    String messageBody;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            messageBody = getArguments().getString("message");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (MessageDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(requireActivity().getLocalClassName() + " must implement MessageDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder
                .setMessage(messageBody)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    listener.onPositiveClick(this);
                })
                .setNegativeButton("CANCEL", ((dialogInterface, i) -> {
                    listener.onNegativeClick(this);
                }));
        return builder.create();
    }
}
