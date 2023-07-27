package de.tuberlin.mcc.simra.app.activities;

import android.app.Dialog;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bugsnag.android.Bugsnag;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.tuberlin.mcc.simra.app.R;

public class LicenseActivity extends AppCompatActivity {

    ImageButton backBtn;
    TextView toolbarTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_library_license);
        toolbarTxt.setTextSize(15.0f);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());

        Button androidSupportLibraryButton = findViewById(R.id.android_support_library);
        createDialogWhenButtonIsPressed(androidSupportLibraryButton, "licenseandroidsupportlibrary.txt", "Android Support Library");

        Button apacheCommonsLibraryButton = findViewById(R.id.apache_commons_library);
        createDialogWhenButtonIsPressed(apacheCommonsLibraryButton, "licenseapachecommons.txt", "Apache Commons");

        Button fbaseButton = findViewById(R.id.fbase_library);
        createDialogWhenButtonIsPressed(fbaseButton, "licensefbase.txt", "FBase");

        Button gsonButton = findViewById(R.id.gson_library);
        createDialogWhenButtonIsPressed(gsonButton, "licensegson.txt", "Gson");

        Button javaxActivationButton = findViewById(R.id.javax_activation_library);
        createDialogWhenButtonIsPressed(javaxActivationButton, "licensejavaxactivation.txt", "Javax Activation");

        Button jerseyButton = findViewById(R.id.jersey_library);
        createDialogWhenButtonIsPressed(jerseyButton, "licensejersey.txt", "Jersey");

        Button jettyButton = findViewById(R.id.jetty_library);
        createDialogWhenButtonIsPressed(jettyButton, "licensejetty.txt", "Jetty");

        Button logbackButton = findViewById(R.id.logback_library);
        createDialogWhenButtonIsPressed(logbackButton, "licenselogback.txt", "Logback");

        Button mpandroidchartButton = findViewById(R.id.mpandroidchart_library);
        createDialogWhenButtonIsPressed(mpandroidchartButton, "licensesmpandroidchart.txt", "MPAndroidChart");

        Button okhttpButton = findViewById(R.id.okhttp_library);
        createDialogWhenButtonIsPressed(okhttpButton, "licenseokhttp.txt", "OkHttp");

        Button osmbonuspackButton = findViewById(R.id.osmbonuspack_library);
        createDialogWhenButtonIsPressed(osmbonuspackButton, "licenseosmbonuspack.txt", "Osmbonuspack");

        Button osmdroidButton = findViewById(R.id.osmdroid_library);
        createDialogWhenButtonIsPressed(osmdroidButton, "licenseosmdroid.txt", "Osmdroid");

        Button rangeSeekBarButton = findViewById(R.id.rangeseekbar_library);
        createDialogWhenButtonIsPressed(rangeSeekBarButton, "licenserangeseekbar.txt", "RangeSeekBar");

        Button slf4jSimpleButton = findViewById(R.id.slf4j_simple_library);
        createDialogWhenButtonIsPressed(slf4jSimpleButton, "licenseslf4jsimple.txt", "SLF4J Simple");

        Button slf4jApiButton = findViewById(R.id.slf4j_api_library);
        createDialogWhenButtonIsPressed(slf4jApiButton, "licenseslf4japi.txt", "SLF4J API");

    }

    private void createDialogWhenButtonIsPressed(Button button, String licenceFileName, String title) {
        Dialog showLicenseDialog = new Dialog(this);
        showLicenseDialog.setContentView(R.layout.show_license_dialog);

        Window window = showLicenseDialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);


        button.setOnClickListener(v -> {
            String message = "";
            try {
                AssetManager am = getApplicationContext().getAssets();
                InputStream is = am.open(licenceFileName);
                InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {

                    stringBuilder.append(receiveString.trim()).append(System.lineSeparator());

                }
                is.close();
                message = stringBuilder.toString();
            } catch (IOException e) {
                Bugsnag.notify(e);
                e.printStackTrace();
            }
            TextView textView = showLicenseDialog.findViewById(R.id.tv);
            textView.setText(message);
            TextView titleView = showLicenseDialog.findViewById(R.id.licenseTitle);
            titleView.setText(title);
            Button closeButton = showLicenseDialog.findViewById(R.id.closeButton);
            closeButton.setOnClickListener(v1 -> showLicenseDialog.dismiss());
            showLicenseDialog.show();
        });
    }
}
