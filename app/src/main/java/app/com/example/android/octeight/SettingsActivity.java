package app.com.example.android.octeight;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences sharedPrefs = getApplicationContext()
                .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPrefs.edit();
        String id = "";


        if (sharedPrefs.contains("USER-ID")) {

            id = sharedPrefs.getString("USER-ID", "00000000");

        } else {

            id = String.valueOf(System.currentTimeMillis());

            editor.putString("USER-ID", id);

            editor.apply();
        }
        TextView tv1 = (TextView)findViewById(R.id.textViewId);

        tv1.setText(id);
    }
}
