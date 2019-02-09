package app.com.example.android.octeight;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SaveActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);
        // While the the Save Activity isnt ready, automatically going back to the Main Activity
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
        // prevent User from going back to the Save Activity
        finish();
    }
}
