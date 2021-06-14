package de.tuberlin.mcc.simra.app.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

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
            int screenSize = getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK;
            if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL) {
                firePrivacyDialogSmallScreen();
            } else {
                firePrivacyDialogNormalScreen();
            }
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
                checkPermissionsAndContinue();
            }
        } else {
            checkPermissionsAndContinue();
        }
    }

    private void checkPermissionsAndContinue() {
        // Check Permissions
        if (allPermissionGranted()) {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            // Call finish() to prevent going back to StartActivity,
            // when the Back Button is pressed in MainActivity
            finish();
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
            checkPermissionsAndContinue();

        });
        alert.setNegativeButton(R.string.no, (dialog, id) -> {
            if (rememberChoice[0]) {
                SharedPref.App.Crash.SendCrashReportAllowed.setDisallowed(this);
            }
            checkPermissionsAndContinue();
        });
        alert.show();
    }

    public void firePrivacyDialogNormalScreen() {
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


    public void firePrivacyDialogSmallScreen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
        AlertDialog dialog = null;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 35, 35, 35);

        TextView title = new TextView(this);
        title.setText(getString(R.string.privacyAgreementTitle));
        title.getTextSize();
        // increase text size of title
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, (title.getTextSize() * 1.2f));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 35);
        layout.addView(title);

        TextView message = new TextView(this);
        message.setText(getResources().getText(R.string.privacyAgreementMessage));
        message.setMovementMethod(LinkMovementMethod.getInstance());
        layout.addView(message);

        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);

        checkBox.setText(getString(R.string.iAccept));
        layout.addView(checkBoxView);

        RelativeLayout buttonsLayout = new RelativeLayout(this);

        MaterialButton negativeButton = new MaterialButton(this);
        RelativeLayout r2 = new RelativeLayout(this);
        r2.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        RelativeLayout.LayoutParams negativeParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        negativeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        negativeButton.setLayoutParams(negativeParams);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Toast.makeText(StartActivity.this, getString(R.string.simra_closed), Toast.LENGTH_LONG).show();
            }
        });
        negativeButton.setText(R.string.close_simra);
        r2.setPadding(35, 0, 0, 0);
        r2.addView(negativeButton);
        buttonsLayout.addView(r2);

        MaterialButton positiveButton = new MaterialButton(this);
        RelativeLayout r1 = new RelativeLayout(this);
        r1.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        RelativeLayout.LayoutParams positiveParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        positiveParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        positiveButton.setLayoutParams(positiveParams);
        positiveButton.setText(R.string.next);
        r1.setPadding(0, 0, 35, 0);
        r1.addView(positiveButton);
        buttonsLayout.addView(r1);


        positiveButton.setEnabled(false);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            positiveButton.setEnabled(isChecked);
        });
        layout.addView(buttonsLayout);
        final ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        builder.setView(scrollView);
        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeBooleanToSharedPrefs("Privacy-Policy-Accepted", checkBox.isChecked(), "simraPrefs", StartActivity.this);
                PermissionHelper.requestFirstBasePermissionsNotGranted(StartActivity.this);
                finalDialog.dismiss();
            }
        });
    }

}
