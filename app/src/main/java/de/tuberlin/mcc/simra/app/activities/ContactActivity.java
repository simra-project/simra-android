package de.tuberlin.mcc.simra.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;

import androidx.appcompat.app.AppCompatActivity;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.ActivityContactBinding;

public class ContactActivity extends AppCompatActivity {

    /**
     * Layout Binding.
     */

    ActivityContactBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityContactBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_contact);

        binding.toolbar.backButton.setOnClickListener(v -> finish());

        // Website
        binding.contentContact.buttonProjectSite.setOnClickListener(view -> {
            Intent intent = new Intent(ContactActivity.this, WebActivity.class);
            intent.putExtra("URL", getString(R.string.link_simra_Page));
            //Intent intent = new Intent(Intent.ACTION_VIEW);
            //intent.setData(Uri.parse(getString(R.string.link_simra_Page)));
            startActivity(intent);
        });
/*        com.google.android.material.button.MaterialButton websiteBtn = findViewById(R.id.button_project_site);
        websiteBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ContactActivity.this, WebActivity.class);
            intent.putExtra("URL", getString(R.string.link_simra_Page));
        });*/
        // Feedback
        binding.contentContact.buttonFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedbackReceiver)});
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedbackHeader));
                intent.putExtra(Intent.EXTRA_TEXT, (getString(R.string.feedbackReceiver)) + System.lineSeparator()
                        + "App Version: " + BuildConfig.VERSION_CODE + System.lineSeparator() + "Android Version: ");
                try {
                    startActivity(Intent.createChooser(intent, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Bugsnag.notify(ex);
                    Toast.makeText(ContactActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }

            }
        });
        // Twitter
       binding.contentContact.buttonTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //intent = new Intent(SocialMediaActivity.this, WebActivity.class);
                intent.setData(Uri.parse(getString(R.string.link_to_twitter)));
                startActivity(intent);
            }
        });
        // Instagram
        binding.contentContact.buttonInstagram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //intent = new Intent(SocialMediaActivity.this, WebActivity.class);
                intent.setData(Uri.parse(getString(R.string.link_to_instagram)));
                startActivity(intent);
            }
        });
        // setContentView(binding.getRoot());
    }

}


/*    String[] textString = {"Item1", "Item2", "Item3", "Item4"};
    int[] drawableIds = {R.drawable.img_id_row1, R.drawable.img_id_row2, R.drawable.img_id_row3, R.drawable.img_id_row4};

    CustomAdapter adapter = new CustomAdapter(this,  textString, drawableIds);


    listView1 = (ListView)findViewById(R.id.menuList);
    listView1.setAdapter(adapter);


public class CustomAdapter extends BaseAdapter {

        private Context mContext;
        private String[]  Title;
        private int[] image;

        public CustomAdapter(Context context, String[] text1,int[] imageId) {
            mContext = context;
            Title = text1;
            image = imageId;

        }


        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = getLayoutInflater();
            View row;
            row = inflater.inflate(R.layout.row, parent, false);
            TextView title;
            ImageView imageview1;
            imageview1 = (ImageView) row.findViewById(R.id.imageIcon);
            title = (TextView) row.findViewById(R.id.titleTxt);
            title.setText(Title[position]);
            imageview1.setImageResource(image[position]);

            return (row);
        }
    }

 */