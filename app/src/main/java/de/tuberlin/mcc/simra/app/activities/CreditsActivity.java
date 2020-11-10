package de.tuberlin.mcc.simra.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityCreditsBinding;

public class CreditsActivity extends AppCompatActivity {

    /**
     * Layout Binding.
     */
    ActivityCreditsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCreditsBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_credits);

        binding.toolbar.backButton.setOnClickListener(v -> finish());
    }

}
