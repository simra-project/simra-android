package de.tuberlin.mcc.simra.app;

import android.app.Dialog;
import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        backBtn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   finish();
               }
            }
        );

        Button androidSupportLibraryButton = findViewById(R.id.android_support_library);
        createDialogWhenButtonIsPressed(androidSupportLibraryButton,"licenseandroidsupportlibrary.txt","Android Support Library");

        Button apacheCommonsLibraryButton = findViewById(R.id.apache_commons_library);
        createDialogWhenButtonIsPressed(apacheCommonsLibraryButton,"licenseapachecommons.txt","Apache Commons");

        Button fbaseButton = findViewById(R.id.fbase_library);
        createDialogWhenButtonIsPressed(fbaseButton,"licensefbase.txt","FBase");

        Button gsonButton = findViewById(R.id.gson_library);
        createDialogWhenButtonIsPressed(gsonButton,"licensegson.txt","Gson");

        Button javaxActivationButton = findViewById(R.id.javax_activation_library);
        createDialogWhenButtonIsPressed(javaxActivationButton,"licensejavaxactivation.txt","Javax Activation");

        Button jerseyButton = findViewById(R.id.jersey_library);
        createDialogWhenButtonIsPressed(jerseyButton,"licensejersey.txt","Jersey");

        Button jettyButton = findViewById(R.id.jetty_library);
        createDialogWhenButtonIsPressed(jettyButton,"licensejetty.txt","Jetty");

        Button logbackButton = findViewById(R.id.logback_library);
        createDialogWhenButtonIsPressed(logbackButton,"licenselogback.txt","Logback");

        Button osmdroidButton = findViewById(R.id.osmdroid_library);
        createDialogWhenButtonIsPressed(osmdroidButton,"licenseosmdroid.txt","Osmdroid");

        Button osmbonuspackButton = findViewById(R.id.osmbonuspack_library);
        createDialogWhenButtonIsPressed(osmbonuspackButton,"licenseosmbonuspack.txt","Osmbonuspack");

        Button okhttpButton = findViewById(R.id.okhttp_library);
        createDialogWhenButtonIsPressed(okhttpButton,"licenseokhttp.txt","OkHttp");

        Button rangeSeekBarButton = findViewById(R.id.rangeseekbar_library);
        createDialogWhenButtonIsPressed(rangeSeekBarButton,"licenserangeseekbar.txt","RangeSeekBar");

        Button slf4jSimpleButton = findViewById(R.id.slf4j_simple_library);
        createDialogWhenButtonIsPressed(slf4jSimpleButton, "licenseslf4jsimple.txt","SLF4J Simple");

        Button slf4jApiButton = findViewById(R.id.slf4j_api_library);
        createDialogWhenButtonIsPressed(slf4jApiButton, "licenseslf4japi.txt","SLF4J API");

    }

    private void createDialogWhenButtonIsPressed(Button button, String licenceFileName, String title) {
        Dialog showLicenseDialog = new Dialog(this);
        showLicenseDialog.setContentView(R.layout.show_license_dialog);

        Window window = showLicenseDialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //perfom actions here
                String message = "";
                try{
                    AssetManager am = getApplicationContext().getAssets();
                    InputStream is = am.open(licenceFileName);
                    InputStreamReader inputStreamReader = new InputStreamReader(is, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    while ( (receiveString = bufferedReader.readLine()) != null ) {

                            stringBuilder.append(receiveString.trim() + System.lineSeparator());

                    }
                    is.close();
                    message = stringBuilder.toString();
                } catch (IOException e) {e.printStackTrace();}
                TextView textView = showLicenseDialog.findViewById(R.id.tv);
                textView.setText(message);
                TextView titleView = showLicenseDialog.findViewById(R.id.licenseTitle);
                titleView.setText(title);
                Button closeButton = showLicenseDialog.findViewById(R.id.closeButton);
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLicenseDialog.dismiss();
                    }
                });
                showLicenseDialog.show();
            }
        });
    }
}
