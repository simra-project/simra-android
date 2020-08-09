package de.tuberlin.mcc.simra.app.annotation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.databinding.IncidentPopupLayoutBinding;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;

public class IncidentPopUpActivity extends AppCompatActivity {
    public static final String EXTRA_INCIDENT = "EXTRA_INCIDENT";
    public static final int REQUEST_CODE = 6034; // This is a random request code, you can safely edit it.
    private static final String EXTRA_RIDE_STATE = "EXTRA_RIDE_STATE";
    private static final String TAG = "IncidentPopUpAct_LOG";

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
        int state = getIntent().getIntExtra(EXTRA_RIDE_STATE, MetaData.STATE.JUST_RECORDED);

        IncidentPopupLayoutBinding binding = IncidentPopupLayoutBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        // Scale the activity so that it is smaller than the activity which called it
        // (ShowRouteActivity), floating on top of it (ShowRouteActivity is still visible
        // in the background thanks to the custom style 'PopUpWindow' in styles.xml)

        // TODO: Use https://stackoverflow.com/questions/38417984/android-spinner-dropdown-checkbox
        binding.incidentTypeSpinner.setSelection(incidentLogEntry.incidentType);
        binding.involvedTypeCheckBoxBus.setChecked(incidentLogEntry.involvedRoadUser.bus);
        binding.involvedTypeCheckBoxCyclist.setChecked(incidentLogEntry.involvedRoadUser.cyclist);
        binding.involvedTypeCheckBoxPedestrian.setChecked(incidentLogEntry.involvedRoadUser.pedestrian);
        binding.involvedTypeCheckBoxDeliveryVan.setChecked(incidentLogEntry.involvedRoadUser.deliveryVan);
        binding.involvedTypeCheckBoxTruck.setChecked(incidentLogEntry.involvedRoadUser.truck);
        binding.involvedTypeCheckBoxMotorcyclist.setChecked(incidentLogEntry.involvedRoadUser.motorcyclist);
        binding.involvedTypeCheckBoxCar.setChecked(incidentLogEntry.involvedRoadUser.car);
        binding.involvedTypeCheckBoxTaxi.setChecked(incidentLogEntry.involvedRoadUser.taxi);
        binding.involvedTypeCheckBoxOther.setChecked(incidentLogEntry.involvedRoadUser.other);
        binding.involvedTypeCheckBoxElectricScooter.setChecked(incidentLogEntry.involvedRoadUser.electricScooter);
        binding.scarinessCheckBox.setChecked(incidentLogEntry.scarySituation);
        binding.incidentDescription.setText(incidentLogEntry.description);

        binding.backButton.setOnClickListener((View v) -> {
            if (state < 2) {
                Toast.makeText(this, getString(R.string.editingIncidentAborted), Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        if (state < 2) {
            binding.saveButton.setOnClickListener((View v) -> {
                // Update the incidentLogEntry with the values from the UI before closing
                incidentLogEntry.incidentType = binding.incidentTypeSpinner.getSelectedItemPosition();
                incidentLogEntry.description = binding.incidentDescription.getText().toString();
                incidentLogEntry.scarySituation = binding.scarinessCheckBox.isChecked();
                incidentLogEntry.involvedRoadUser.bus = binding.involvedTypeCheckBoxBus.isChecked();
                incidentLogEntry.involvedRoadUser.cyclist = binding.involvedTypeCheckBoxCyclist.isChecked();
                incidentLogEntry.involvedRoadUser.pedestrian = binding.involvedTypeCheckBoxPedestrian.isChecked();
                incidentLogEntry.involvedRoadUser.deliveryVan = binding.involvedTypeCheckBoxDeliveryVan.isChecked();
                incidentLogEntry.involvedRoadUser.truck = binding.involvedTypeCheckBoxTruck.isChecked();
                incidentLogEntry.involvedRoadUser.motorcyclist = binding.involvedTypeCheckBoxMotorcyclist.isChecked();
                incidentLogEntry.involvedRoadUser.car = binding.involvedTypeCheckBoxCar.isChecked();
                incidentLogEntry.involvedRoadUser.taxi = binding.involvedTypeCheckBoxTaxi.isChecked();
                incidentLogEntry.involvedRoadUser.other = binding.involvedTypeCheckBoxOther.isChecked();
                incidentLogEntry.involvedRoadUser.electricScooter = binding.involvedTypeCheckBoxElectricScooter.isChecked();

                Toast.makeText(this, getString(R.string.editingIncidentCompleted), Toast.LENGTH_SHORT).show();

                Intent returnIntent = new Intent();
                returnIntent.putExtra(EXTRA_INCIDENT, incidentLogEntry);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            });
        } else {
            binding.saveButton.setVisibility(View.GONE);
            binding.incidentTypeSpinner.setEnabled(false);
            binding.incidentDescription.setEnabled(false);
            binding.scarinessCheckBox.setEnabled(false);
            binding.involvedTypeCheckBoxBus.setEnabled(false);
            binding.involvedTypeCheckBoxCyclist.setEnabled(false);
            binding.involvedTypeCheckBoxPedestrian.setEnabled(false);
            binding.involvedTypeCheckBoxDeliveryVan.setEnabled(false);
            binding.involvedTypeCheckBoxTruck.setEnabled(false);
            binding.involvedTypeCheckBoxMotorcyclist.setEnabled(false);
            binding.involvedTypeCheckBoxCar.setEnabled(false);
            binding.involvedTypeCheckBoxTaxi.setEnabled(false);
            binding.involvedTypeCheckBoxOther.setEnabled(false);
            binding.involvedTypeCheckBoxElectricScooter.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
