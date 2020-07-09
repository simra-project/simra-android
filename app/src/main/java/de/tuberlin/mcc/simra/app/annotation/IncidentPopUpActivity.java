package de.tuberlin.mcc.simra.app.annotation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;

public class IncidentPopUpActivity extends AppCompatActivity {
    public static final String EXTRA_INCIDENT = "EXTRA_INCIDENT";
    public static final int REQUEST_CODE = 6034; // This is a random request code, you can safely edit it.
    private static final String EXTRA_RIDE_STATE = "EXTRA_RIDE_STATE";
    private static final String TAG = "IncidentPopUpAct_LOG";
    LinearLayout doneButton;
    LinearLayout backButton;
    RelativeLayout exitButton;
    Boolean incidentSaved = false;
    int state = 0;

    private IncidentLogEntry incidentLogEntry;

    public static Integer startIncidentPopUpActivity(IncidentLogEntry incidentLogEntry, int state, Activity activity) {
        Intent intent = new Intent(activity, IncidentPopUpActivity.class);
        intent.putExtra(EXTRA_INCIDENT, incidentLogEntry);
        intent.putExtra(EXTRA_RIDE_STATE, state);
        activity.startActivityForResult(intent, REQUEST_CODE);
        return REQUEST_CODE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        incidentLogEntry = (IncidentLogEntry) getIntent().getSerializableExtra(EXTRA_INCIDENT);
        state = getIntent().getIntExtra(EXTRA_RIDE_STATE, MetaData.STATE.JUST_RECORDED);
        if (state < MetaData.STATE.SYNCED) {
            setContentView(R.layout.incident_popup_layout);
        } else {
            setContentView(R.layout.incident_popup_layout_uneditable_incident);
        }

        // Scale the activity so that it is smaller than the activity which called it
        // (ShowRouteActivity), floating on top of it (ShowRouteActivity is still visible
        // in the background thanks to the custom style 'PopUpWindow' in styles.xml)

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);

        Spinner incidentTypeSpinner = findViewById(R.id.incidentTypeSpinner);
        EditText incidentDescription = findViewById(R.id.EditTextDescriptionBody);
        CheckBox scarinessCheckBox = findViewById(R.id.scarinessCheckBox);
        CheckBox involvedTypeCheckBoxBus = findViewById(R.id.involvedType1);
        CheckBox involvedTypeCheckBoxCyclist = findViewById(R.id.involvedType2);
        CheckBox involvedTypeCheckBoxPedestrian = findViewById(R.id.involvedType3);
        CheckBox involvedTypeCheckBoxDeliveryVan = findViewById(R.id.involvedType4);
        CheckBox involvedTypeCheckBoxTruck = findViewById(R.id.involvedType5);
        CheckBox involvedTypeCheckBoxMotorcyclist = findViewById(R.id.involvedType6);
        CheckBox involvedTypeCheckBoxCar = findViewById(R.id.involvedType7);
        CheckBox involvedTypeCheckBoxTaxi = findViewById(R.id.involvedType8);
        CheckBox involvedTypeCheckBoxOther = findViewById(R.id.involvedType9);
        CheckBox involvedTypeCheckBoxElectricScooter = findViewById(R.id.involvedType10);

        // TODO: Use https://stackoverflow.com/questions/38417984/android-spinner-dropdown-checkbox
        incidentTypeSpinner.setSelection(incidentLogEntry.incidentType);
        involvedTypeCheckBoxBus.setChecked(incidentLogEntry.involvedRoadUser.bus);
        involvedTypeCheckBoxCyclist.setChecked(incidentLogEntry.involvedRoadUser.cyclist);
        involvedTypeCheckBoxPedestrian.setChecked(incidentLogEntry.involvedRoadUser.pedestrian);
        involvedTypeCheckBoxDeliveryVan.setChecked(incidentLogEntry.involvedRoadUser.deliveryVan);
        involvedTypeCheckBoxTruck.setChecked(incidentLogEntry.involvedRoadUser.truck);
        involvedTypeCheckBoxMotorcyclist.setChecked(incidentLogEntry.involvedRoadUser.motorcyclist);
        involvedTypeCheckBoxCar.setChecked(incidentLogEntry.involvedRoadUser.car);
        involvedTypeCheckBoxTaxi.setChecked(incidentLogEntry.involvedRoadUser.taxi);
        involvedTypeCheckBoxOther.setChecked(incidentLogEntry.involvedRoadUser.other);
        involvedTypeCheckBoxElectricScooter.setChecked(incidentLogEntry.involvedRoadUser.electricScooter);
        scarinessCheckBox.setChecked(incidentLogEntry.scarySituation);
        incidentDescription.setText(incidentLogEntry.description);

        if (state == 2) {
            incidentTypeSpinner.setEnabled(false);
            incidentDescription.setEnabled(false);
            scarinessCheckBox.setEnabled(false);
            involvedTypeCheckBoxBus.setEnabled(false);
            involvedTypeCheckBoxCyclist.setEnabled(false);
            involvedTypeCheckBoxPedestrian.setEnabled(false);
            involvedTypeCheckBoxDeliveryVan.setEnabled(false);
            involvedTypeCheckBoxTruck.setEnabled(false);
            involvedTypeCheckBoxMotorcyclist.setEnabled(false);
            involvedTypeCheckBoxCar.setEnabled(false);
            involvedTypeCheckBoxTaxi.setEnabled(false);
            involvedTypeCheckBoxOther.setEnabled(false);
            involvedTypeCheckBoxElectricScooter.setEnabled(false);
        }

        if (state < 2) {
            doneButton = findViewById(R.id.save_button);
            backButton = findViewById(R.id.back_button);

            doneButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    doneButton.setElevation(0.0f);
                    doneButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    doneButton.setElevation(2 * IncidentPopUpActivity.this.getResources().getDisplayMetrics().density);
                    doneButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            });

            backButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    backButton.setElevation(0.0f);
                    backButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    backButton.setElevation(2 * IncidentPopUpActivity.this.getResources().getDisplayMetrics().density);
                    backButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            });

            if (getIntent().getExtras() != null) {

                String lat = getIntent().getStringExtra("Incident_latitude");
                String lon = getIntent().getStringExtra("Incident_longitude");
                String ts = getIntent().getStringExtra("Incident_timeStamp");
                String bike = getIntent().getStringExtra("Incident_bike");
                String child = getIntent().getStringExtra("Incident_child");
                String trailer = getIntent().getStringExtra("Incident_trailer");
                String pLoc = getIntent().getStringExtra("Incident_pLoc");

                // onClick-behavior for 'Done inserting description'-button: save incident
                // data to file.

                doneButton.setOnClickListener((View v) -> {
                    // Update the incidentLogEntry with the values from the UI before closing
                    incidentLogEntry.incidentType = incidentTypeSpinner.getSelectedItemPosition();
                    incidentLogEntry.description = incidentDescription.getText().toString();
                    incidentLogEntry.scarySituation = scarinessCheckBox.isChecked();
                    incidentLogEntry.involvedRoadUser.bus = involvedTypeCheckBoxBus.isChecked();
                    incidentLogEntry.involvedRoadUser.cyclist = involvedTypeCheckBoxCyclist.isChecked();
                    incidentLogEntry.involvedRoadUser.pedestrian = involvedTypeCheckBoxPedestrian.isChecked();
                    incidentLogEntry.involvedRoadUser.deliveryVan = involvedTypeCheckBoxDeliveryVan.isChecked();
                    incidentLogEntry.involvedRoadUser.truck = involvedTypeCheckBoxTruck.isChecked();
                    incidentLogEntry.involvedRoadUser.motorcyclist = involvedTypeCheckBoxMotorcyclist.isChecked();
                    incidentLogEntry.involvedRoadUser.car = involvedTypeCheckBoxCar.isChecked();
                    incidentLogEntry.involvedRoadUser.taxi = involvedTypeCheckBoxTaxi.isChecked();
                    incidentLogEntry.involvedRoadUser.other = involvedTypeCheckBoxOther.isChecked();
                    incidentLogEntry.involvedRoadUser.electricScooter = involvedTypeCheckBoxElectricScooter.isChecked();

                    incidentSaved = true;
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(EXTRA_INCIDENT, incidentLogEntry);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                });

                // Return to ShowRouteActivity without saving the annotated incidents
                backButton.setOnClickListener((View v) -> {
                    incidentSaved = false;
                    finish();
                });

            }
        } else {
            exitButton = findViewById(R.id.exitButton);
            exitButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    exitButton.setElevation(0.0f);
                    exitButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    exitButton.setElevation(2 * IncidentPopUpActivity.this.getResources().getDisplayMetrics().density);
                    exitButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            });
            exitButton.setOnClickListener(v -> {
                incidentSaved = false;
                finish();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        if (state < 2) {
            if (incidentSaved) {
                Toast.makeText(this, getString(R.string.editingIncidentCompleted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editingIncidentAborted), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
