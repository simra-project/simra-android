package app.com.example.android.octeight;


import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class LicensesActivity extends ListActivity {

    private String dataArray[];

    public LicensesActivity() {
        dataArray = new String[] { "One", "Two", "Three", };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListAdapter listAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, dataArray);
        setListAdapter(listAdapter);
    }

    @Override
    public void onListItemClick(ListView list, View v, int position, long id) {

        Toast.makeText(this,
                getListView().getItemAtPosition(position).toString(),
                Toast.LENGTH_LONG).show();
    }
}