package de.tuberlin.mcc.simra.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
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

        String[] items = getResources().getStringArray(R.array.aboutSimraItems);
        binding.listView.setAdapter(new ArrayAdapter<>(AboutActivity.this,
                android.R.layout.simple_list_item_1, items));
        binding.listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = null;
            switch (position) {
                case 0:
                    intent = new Intent(AboutActivity.this, WebActivity.class);
                    intent.putExtra("URL", getString(R.string.link_simra_Page));
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
