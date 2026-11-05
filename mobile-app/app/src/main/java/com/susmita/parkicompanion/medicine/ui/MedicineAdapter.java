package com.susmita.parkicompanion.medicine.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.medicine.model.Medicine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    public interface OnMedicineClickListener {
        void onItemClick(Medicine medicine);
        void onToggleEnable(Medicine medicine, boolean isEnabled);
        void onDeleteClick(Medicine medicine);
    }

    private List<Medicine> medicineList = new ArrayList<>();
    private final OnMedicineClickListener listener;

    public MedicineAdapter(OnMedicineClickListener listener) {
        this.listener = listener;
    }

    public void setMedicines(List<Medicine> medicines) {
        this.medicineList = medicines != null ? medicines : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        holder.bind(medicineList.get(position));
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    class MedicineViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivItemMedicinePic;
        private final TextView tvMedicineName;
        private final TextView tvMedicineInstruction;
        private final TextView tvMedicineDose;
        private final TextView tvMedicineTimes;
        private final TextView tvMedicineRepeat;
        private final TextView tvMedicineNotes;
        private final TextView tvSwitchLabel;
        private final SwitchMaterial switchMedicineToggle;
        private final ImageButton btnDeleteMedicine;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemMedicinePic = itemView.findViewById(R.id.ivItemMedicinePic);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            tvMedicineInstruction = itemView.findViewById(R.id.tvMedicineInstruction);
            tvMedicineDose = itemView.findViewById(R.id.tvMedicineDose);
            tvMedicineTimes = itemView.findViewById(R.id.tvMedicineTimes);
            tvMedicineRepeat = itemView.findViewById(R.id.tvMedicineRepeat);
            tvMedicineNotes = itemView.findViewById(R.id.tvMedicineNotes);
            tvSwitchLabel = itemView.findViewById(R.id.tvSwitchLabel);
            switchMedicineToggle = itemView.findViewById(R.id.switchMedicineToggle);
            btnDeleteMedicine = itemView.findViewById(R.id.btnDeleteMedicine);
        }

        public void bind(Medicine medicine) {
            tvMedicineName.setText(medicine.getName());

            if (medicine.getInstruction() != null && !medicine.getInstruction().trim().isEmpty()) {
                tvMedicineInstruction.setVisibility(View.VISIBLE);
                tvMedicineInstruction.setText("Instruction • " + medicine.getInstruction());
            } else {
                tvMedicineInstruction.setVisibility(View.GONE);
            }

            // Load medicine picture if available
            String imageUriStr = medicine.getImageUri();
            if (imageUriStr != null && !imageUriStr.trim().isEmpty()) {
                try {
                    File imgFile = new File(imageUriStr);
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        if (bitmap != null) {
                            ivItemMedicinePic.setImageBitmap(bitmap);
                            ivItemMedicinePic.setVisibility(View.VISIBLE);
                        } else {
                            ivItemMedicinePic.setVisibility(View.GONE);
                        }
                    } else {
                        Uri uri = Uri.parse(imageUriStr);
                        ivItemMedicinePic.setImageURI(uri);
                        ivItemMedicinePic.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    ivItemMedicinePic.setVisibility(View.GONE);
                }
            } else {
                ivItemMedicinePic.setVisibility(View.GONE);
            }

            String doseText = "Dose • " + (medicine.getDosage() != null && !medicine.getDosage().isEmpty() ? medicine.getDosage() : "1 tablet");
            tvMedicineDose.setText(doseText);

            String timesText = "Times • " + (medicine.getTimes() != null && !medicine.getTimes().isEmpty() ? medicine.getTimes() : "Not set");
            tvMedicineTimes.setText(timesText);

            String repeatText;
            if (medicine.getSpecificDate() != null && !medicine.getSpecificDate().trim().isEmpty()) {
                repeatText = "Date • " + medicine.getSpecificDate();
            } else if (medicine.getRepeatDays() != null && !medicine.getRepeatDays().trim().isEmpty()) {
                repeatText = "Repeat • " + medicine.getRepeatDays();
            } else {
                repeatText = "Repeat • Everyday";
            }
            tvMedicineRepeat.setText(repeatText);

            if (medicine.getNotes() != null && !medicine.getNotes().trim().isEmpty()) {
                tvMedicineNotes.setVisibility(View.VISIBLE);
                tvMedicineNotes.setText("Notes • " + medicine.getNotes());
            } else {
                tvMedicineNotes.setVisibility(View.GONE);
            }

            switchMedicineToggle.setOnCheckedChangeListener(null);
            switchMedicineToggle.setChecked(medicine.isEnabled());
            tvSwitchLabel.setText(medicine.isEnabled() ? "ON" : "OFF");

            switchMedicineToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                tvSwitchLabel.setText(isChecked ? "ON" : "OFF");
                if (listener != null) {
                    listener.onToggleEnable(medicine, isChecked);
                }
            });

            btnDeleteMedicine.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(medicine);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(medicine);
                }
            });
        }
    }
}