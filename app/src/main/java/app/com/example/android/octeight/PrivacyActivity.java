package app.com.example.android.octeight;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import static app.com.example.android.octeight.Utils.writeBooleanToSharedPrefs;

public class PrivacyActivity extends AppCompatActivity {


    ImageButton backBtn;
    TextView toolbarTxt;
    Boolean privacyAgreement;
    // Context ctx = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);
        //ctx = getBaseContext();

        SharedPreferences sharedPrefs = getApplicationContext()
                .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (!sharedPrefs.contains("Data-Privacy-Agreement")) {
            editor.putBoolean("Data-Privacy-Agreement", false);
            editor.commit();
        }
        privacyAgreement = sharedPrefs.getBoolean("Data-Privacy-Agreement", false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_privacy);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           if (!privacyAgreement) {
                                               privacyAgreement = getAgreementValueBack();
                                               if (privacyAgreement) {
                                                   writeBooleanToSharedPrefs("Data-Privacy-Agreement", true, "simraPrefs", PrivacyActivity.this);
                                               }
                                           }
                                           finish();
                                       }
                                   }
        );

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PrivacyActivity.this, WebActivity.class);
                intent.putExtra("URL", getString(R.string.dsbContact));
                startActivity(intent);
            }
        });
    }

    public boolean getAgreementValueBack(){ // Context callerContext) {

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException();
            }
        };

        AlertDialog.Builder alert = new AlertDialog.Builder(PrivacyActivity.this);
        alert.setTitle(getString(R.string.dataPrivayAgreementDialogTitle));
        alert.setMessage(getString(R.string.dataPrivayAgreementDialogText));
        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                privacyAgreement = true;
                handler.sendMessage(handler.obtainMessage());
                // finish();
            }
        });
        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                privacyAgreement = false;
                handler.sendMessage(handler.obtainMessage());
            }
        });
        alert.show();

        try {
            Looper.loop();
        } catch (RuntimeException e) {
        }

        return privacyAgreement;
    }
}
