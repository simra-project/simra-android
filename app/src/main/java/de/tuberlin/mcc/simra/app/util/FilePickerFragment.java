package de.tuberlin.mcc.simra.app.util;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import de.tuberlin.mcc.simra.app.R;

public class FilePickerFragment extends Fragment {
    public static final int PICKFILE_RESULT_CODE = 1;

    private static final String TAG = "FilePickerFragment_LOG";

    private Button btnChooseFile;
    private TextView tvItemPath;

    private Uri fileUri;
    private String filePath;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.choose_file, container, false);
        Log.d(TAG,"inflated");
        btnChooseFile = (Button) rootView.findViewById(R.id.btn_choose_file);
        tvItemPath = (TextView) rootView.findViewById(R.id.tv_file_path);

        btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Button in FilePicker clicked");
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
            }
        });
        Log.d(TAG,"return" +rootView.toString());
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == -1) {
                    fileUri = data.getData();
                    filePath = fileUri.getPath();
                    tvItemPath.setText(filePath);
                }

                break;
        }
    }
}
