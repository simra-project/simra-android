package de.tuberlin.mcc.simra.app.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.util.Arrays;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.services.UploadService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.PermissionHelper;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UpdateHelper;

import static de.tuberlin.mcc.simra.app.util.LogHelper.showDataDirectory;
import static de.tuberlin.mcc.simra.app.util.LogHelper.showKeyPrefs;
import static de.tuberlin.mcc.simra.app.util.LogHelper.showMetadata;
import static de.tuberlin.mcc.simra.app.util.LogHelper.showStatistics;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.deleteErrorLogsForVersion;

/**
 * Shows general info about the app, if the app is run the first time.
 * If not, MainActivity is started, except if a crash happened before.
 */

public class StartActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "StartActivity_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Just some Logs
        Log.d(TAG, "getFilesDir(): " + Arrays.toString(new File(getFilesDir(), "../shared_prefs").listFiles()));
        Log.d(TAG, "onCreate() started");
        showKeyPrefs(this);
        showDataDirectory(this);
        showMetadata(this);
        showStatistics(this);
        deleteErrorLogsForVersion(this, 26);

        // IMPORTANT: Do not remove!
        UpdateHelper.migrate(this);

        // Check Permissions
        if (allPermissionGranted()) {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            // Call finish() to prevent going back to StartActivity,
            // when the Back Button is pressed in MainActivity
            finish();
        }
        showErrorDialogIfCrashedBefore();

        Button next = findViewById(R.id.nextBtn);
        next.setOnClickListener(v -> {
            navigateIfAllPermissionsGranted();
        });
    }

    public boolean allPermissionGranted() {
        return privacyPolicyAccepted() && PermissionHelper.hasBasePermissions(this);
    }

    public void navigateIfAllPermissionsGranted() {
        if (allPermissionGranted()) {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            // Call finish() to prevent going back to StartActivity,
            // when the Back Button is pressed in MainActivity
            finish();
        } else {
            firePrivacyDialog();
        }
    }

    private boolean privacyPolicyAccepted() {
        return lookUpBooleanSharedPrefs("Privacy-Policy-Accepted", false, "simraPrefs", this);
    }

    /**
     * Look up whether to ask the user for a permission to
     * send the crash logs to the server. If the user gives permission, upload the crash report(s).
     */
    private void showErrorDialogIfCrashedBefore() {
        if (SharedPref.App.Crash.NewCrash.isActive(this)) {
            if (SharedPref.App.Crash.SendCrashReportAllowed.isUnknown(this)) {
                fireSendErrorDialog();
            } else if (SharedPref.App.Crash.SendCrashReportAllowed.isAllowed(this)) {
                Intent intent = new Intent(StartActivity.this, UploadService.class);
                intent.putExtra("CRASH_REPORT", true);
                startService(intent);
            }
        }
    }

    public void fireSendErrorDialog() {

        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
        final boolean[] rememberChoice = {false};
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> rememberChoice[0] = isChecked);
        checkBox.setText(getString(R.string.rememberMyChoice));
        AlertDialog.Builder alert = new AlertDialog.Builder(StartActivity.this);
        alert.setTitle(getString(R.string.sendErrorTitle));
        alert.setMessage(getString(R.string.sendErrorMessage));
        alert.setView(checkBoxView);
        alert.setPositiveButton(R.string.yes, (dialog, id) -> {
            if (rememberChoice[0]) {
                SharedPref.App.Crash.SendCrashReportAllowed.setAllowed(this);
            }
            Intent intent = new Intent(StartActivity.this, UploadService.class);
            intent.putExtra("CRASH_REPORT", true);
            startService(intent);
        });
        alert.setNegativeButton(R.string.no, (dialog, id) -> {
            if (rememberChoice[0]) {
                SharedPref.App.Crash.SendCrashReportAllowed.setDisallowed(this);
            }
        });
        alert.show();
    }


    public void firePrivacyDialog() {
        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);

        checkBox.setText(getString(R.string.iAccept));
        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);

        // Linkify the message
        builder.setTitle(getString(R.string.privacyAgreementTitle));
        builder.setMessage(getResources().getText(R.string.privacyAgreementMessage));
        builder.setView(checkBoxView);
        DialogInterface.OnClickListener positiveButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                writeBooleanToSharedPrefs("Privacy-Policy-Accepted", checkBox.isChecked(), "simraPrefs", StartActivity.this);
                PermissionHelper.requestFirstBasePermissionsNotGranted(StartActivity.this);
            }
        };
        builder.setPositiveButton(R.string.next, positiveButtonListener);
        builder.setNegativeButton(R.string.close_simra, (dialog, id) -> {
            finish();
            Toast.makeText(StartActivity.this, getString(R.string.simra_closed), Toast.LENGTH_LONG).show();
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

        // Initially disable the button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));
    }

}
