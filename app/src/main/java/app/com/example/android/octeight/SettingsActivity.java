package app.com.example.android.octeight;

import android.os.Bundle;
import android.widget.TextView;

import static app.com.example.android.octeight.Utils.getUniqueUserID;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        TextView tv1 = findViewById(R.id.textViewId);
        tv1.setText(getUniqueUserID(this));
    }
}
