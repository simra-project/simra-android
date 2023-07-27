package de.tuberlin.mcc.simra.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityFeedbackBinding;

public class FeedbackActivity extends AppCompatActivity {

    /**
     * Layout Binding.
     */
    ActivityFeedbackBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_feedback);


        binding = ActivityFeedbackBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_about_simra);

        binding.toolbar.backButton.setOnClickListener(v -> finish());

        String[] items = getResources().getStringArray(R.array.ContactItems);
        binding.listView.setAdapter(new ArrayAdapter<>(de.tuberlin.mcc.simra.app.activities.FeedbackActivity.this,
                android.R.layout.simple_list_item_1, items));
        binding.listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = null;
            switch (position) {
                case 0:
                    intent = new Intent(FeedbackActivity.this, WebActivity.class);
                    intent.putExtra("URL", getString(R.string.link_simra_Page));
                    break;
                case 1:
                    intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("message/rfc822");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedbackReceiver)});
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedbackHeader));
                    intent.putExtra(Intent.EXTRA_TEXT, (getString(R.string.feedbackReceiver)) + System.lineSeparator()
                            + "App Version: " + BuildConfig.VERSION_CODE + System.lineSeparator() + "Android Version: ");
                    try {
                        startActivity(Intent.createChooser(intent, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Bugsnag.notify(ex);
                        Toast.makeText(FeedbackActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    intent = new Intent(Intent.ACTION_VIEW);
                    //intent = new Intent(SocialMediaActivity.this, WebActivity.class);
                    intent.setData(Uri.parse(getString(R.string.link_to_twitter)));
                    startActivity(intent);
                    break;
                case 3:
                    intent = new Intent(Intent.ACTION_VIEW);
                    //intent = new Intent(SocialMediaActivity.this, WebActivity.class);
                    intent.setData(Uri.parse(getString(R.string.link_to_instagram)));
                    startActivity(intent);
                    break;
                default:
                    Toast.makeText(FeedbackActivity.this, R.string.notReady, Toast.LENGTH_SHORT).show();
            }
            if (intent != null) {
                startActivity(intent);
            }


        });

    }
}
