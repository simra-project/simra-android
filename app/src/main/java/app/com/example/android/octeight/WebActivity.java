package app.com.example.android.octeight;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class WebActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        WebView myWebView = new WebView(getBaseContext());
        setContentView(myWebView);
        myWebView.loadUrl(getString(R.string.mccPageDE));
    }
}
