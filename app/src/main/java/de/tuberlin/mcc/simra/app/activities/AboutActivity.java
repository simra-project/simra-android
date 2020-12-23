package de.tuberlin.mcc.simra.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityAboutBinding;
import de.tuberlin.mcc.simra.app.util.BaseActivity;

/**
 * About Activity displaying information about licenses and privacy policies.
 */
public class AboutActivity extends BaseActivity {

    /**
     * Layout Binding.
     */
    ActivityAboutBinding binding;
    ImageButton helmetBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAboutBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_about_simra);

        binding.toolbar.backButton.setOnClickListener(v -> finish());

        helmetBtn = findViewById(R.id.helmet_icon);

        helmetBtn.setOnClickListener(v -> {
            if (findViewById(R.id.button1).getVisibility() == View.VISIBLE) {
                findViewById(R.id.button1).setVisibility(View.GONE);
                // findViewById(R.id.button2).setVisibility(View.GONE);
            } else {
                findViewById(R.id.button1).setVisibility(View.VISIBLE);
                // findViewById(R.id.button2).setVisibility(View.VISIBLE);

                Toast.makeText(AboutActivity.this, R.string.toast_help_clicked, Toast.LENGTH_LONG)
                        .show();
            }
        });
        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(v -> {
            findViewById(R.id.button1).setVisibility(View.GONE);
        });

        String[] items = getResources().getStringArray(R.array.aboutSimraItems);
        binding.listView.setAdapter(new ArrayAdapter<>(AboutActivity.this,
                android.R.layout.simple_list_item_1, items));
        binding.listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = null;
            switch (position) {
                case 0:
                    intent = new Intent(AboutActivity.this, WebActivity.class);
                    intent.putExtra("URL", getString(R.string.simRaPage));
                    break;
                case 1:
                    intent = new Intent(AboutActivity.this, WebActivity.class);
                    intent.putExtra("URL", getString(R.string.privacyLink));

                    break;
                case 2:
                    intent = new Intent(AboutActivity.this, LicenseActivity.class);
                    break;
                case 3:
                    intent = new Intent(AboutActivity.this, CreditsActivity.class);
                    break;
                default:
                    Toast.makeText(AboutActivity.this, R.string.notReady, Toast.LENGTH_SHORT).show();
            }
            if (intent != null) {
                startActivity(intent);
            }


        });

    }
}
