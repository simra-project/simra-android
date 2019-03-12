package app.com.example.android.octeight;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import static app.com.example.android.octeight.Utils.lookUpSharedPrefs;
import static app.com.example.android.octeight.Utils.writeToSharedPrefs;

public class ImprintActivity extends AppCompatActivity {

    Button privacyBtn;
    Button creditsBtn;
    ImageButton backBtn;
    TextView toolbarTxt;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imprint);
        context = getApplicationContext();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.imprint);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           finish();
                                       }
                                   }
        );
        privacyBtn = findViewById(R.id.privacyButton);
        privacyBtn.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              Intent intent = new Intent(ImprintActivity.this, PrivacyActivity.class);
                                              startActivity(intent);
                                          }
                                      }
        );
        creditsBtn = findViewById(R.id.creditsButton);
        creditsBtn.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              Intent intent = new Intent(ImprintActivity.this, CreditsActivity.class);
                                              startActivity(intent);
                                          }
                                      }
        );

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedbackReceiver)});
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedbackHeader));
                i.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedbackReceiver) + "\n id: " + getUniqueUserID(context));
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(ImprintActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static String getUniqueUserID(Context context) {
        String id = lookUpSharedPrefs("USER-ID", "0", "simraPrefs", context);
        if (id.equals("0")) {
            id = String.valueOf(System.currentTimeMillis());
            writeToSharedPrefs("USER-ID", id, "simraPrefs", context);
        }
        return id;
    }
}
