package com.hci.sroettering.spotifyrc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by sroettering on 06.06.16.
 */
public class ExperimentInfoDialog extends DialogFragment {

    // Use this instance of the interface to deliver action events
    private ExperimentDialogListener mListener;

    public static ExperimentInfoDialog newInstance(String subjID, String scenID) {
        ExperimentInfoDialog dialog = new ExperimentInfoDialog();
        Bundle args = new Bundle();
        args.putString("subjectID", subjID);
        args.putString("scenarioID", scenID);
        dialog.setArguments(args);
        return dialog;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String subjID = getArguments().getString("subjectID");
        String scenID = getArguments().getString("scenarioID");

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.experiment_info_dialog, null);

        EditText subjIDField = (EditText) dialogView.findViewById(R.id.expinfo_subjid);
        subjIDField.setText(subjID);

        EditText scenIDField = (EditText) dialogView.findViewById(R.id.expinfo_scenid);
        scenIDField.setText(scenID);

        builder.setView(dialogView)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogPositiveClick(ExperimentInfoDialog.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogNegativeClick(ExperimentInfoDialog.this);
                    }
                });

        return builder.create();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ExperimentDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }


    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface ExperimentDialogListener {
        public void onDialogPositiveClick(ExperimentInfoDialog dialog);
        public void onDialogNegativeClick(ExperimentInfoDialog dialog);
    }



}
