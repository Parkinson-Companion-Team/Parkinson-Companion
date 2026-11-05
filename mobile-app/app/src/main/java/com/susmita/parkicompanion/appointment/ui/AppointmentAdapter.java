package com.susmita.parkicompanion.appointment.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.appointment.model.Appointment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppointmentAdapter extends ListAdapter<Appointment, AppointmentAdapter.ViewHolder> {

    public interface OnAppointmentClickListener {
        void onItemClick(Appointment appointment);
        void onDeleteClick(Appointment appointment);
        void onAlarmToggle(Appointment appointment, boolean isChecked);
    }

    private final OnAppointmentClickListener listener;

    public AppointmentAdapter(OnAppointmentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Appointment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Appointment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getTimeInMillis() == newItem.getTimeInMillis() &&
                    oldItem.isAlarmSet() == newItem.isAlarmSet() &&
                    oldItem.getReminderOffsetMinutes() == newItem.getReminderOffsetMinutes();
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = getItem(position);
        holder.bind(appointment, listener);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvDateTime;
        private final TextView tvDoctor;
        private final TextView tvLocation;
        private final TextView tvPhone;
        private final View layoutPhoneRow;
        private final TextView tvPrescription;
        private final TextView tvNotes;
        private final SwitchCompat switchAlarm;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;
        private final Button btnMap;
        private final Button btnCall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvDateTime = itemView.findViewById(R.id.tvItemDateTime);
            tvDoctor = itemView.findViewById(R.id.tvItemDoctor);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvPhone = itemView.findViewById(R.id.tvItemPhone);
            layoutPhoneRow = itemView.findViewById(R.id.layoutPhoneRow);
            tvPrescription = itemView.findViewById(R.id.tvItemPrescription);
            tvNotes = itemView.findViewById(R.id.tvItemNotes);
            switchAlarm = itemView.findViewById(R.id.switchAlarm);
            btnEdit = itemView.findViewById(R.id.btnItemEdit);
            btnDelete = itemView.findViewById(R.id.btnItemDelete);
            btnMap = itemView.findViewById(R.id.btnItemMap);
            btnCall = itemView.findViewById(R.id.btnItemCall);
        }

        public void bind(Appointment appointment, OnAppointmentClickListener listener) {
            tvTitle.setText(appointment.getTitle());

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
            String dateStr = sdf.format(new Date(appointment.getTimeInMillis()));
            tvDateTime.setText("📅 " + dateStr + " (" + appointment.getReminderOffsetMinutes() + "m before)");

            tvDoctor.setText(appointment.getDoctorName() != null && !appointment.getDoctorName().isEmpty() ? "👨‍⚕️ " + appointment.getDoctorName() : "👨‍⚕️ Doctor: N/A");
            tvLocation.setText(appointment.getLocation() != null && !appointment.getLocation().isEmpty() ? "📍 " + appointment.getLocation() : "📍 Location: N/A");

            if (appointment.getPhone() != null && !appointment.getPhone().trim().isEmpty()) {
                layoutPhoneRow.setVisibility(View.VISIBLE);
                tvPhone.setText("📞 " + appointment.getPhone());
                btnCall.setOnClickListener(v -> {
                    try {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + appointment.getPhone()));
                        itemView.getContext().startActivity(dialIntent);
                    } catch (Exception e) {
                        Toast.makeText(itemView.getContext(), "Could not open dialer", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                layoutPhoneRow.setVisibility(View.GONE);
            }

            if (appointment.getPrescriptionUri() != null && !appointment.getPrescriptionUri().trim().isEmpty()) {
                tvPrescription.setVisibility(View.VISIBLE);
                String[] files = appointment.getPrescriptionUri().split("\\|");
                tvPrescription.setText("📄 View Prescription / Files (" + files.length + ") ↗");
                tvPrescription.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), PrescriptionViewerActivity.class);
                    intent.putExtra(PrescriptionViewerActivity.EXTRA_PRESCRIPTION_URIS, appointment.getPrescriptionUri());
                    v.getContext().startActivity(intent);
                });
            } else {
                tvPrescription.setVisibility(View.GONE);
            }

            if (appointment.getNotes() != null && !appointment.getNotes().isEmpty()) {
                tvNotes.setVisibility(View.VISIBLE);
                tvNotes.setText("Notes: " + appointment.getNotes());
            } else {
                tvNotes.setVisibility(View.GONE);
            }

            btnMap.setOnClickListener(v -> {
                String loc = appointment.getLocation();
                if (loc == null || loc.trim().isEmpty()) {
                    Toast.makeText(itemView.getContext(), "No location specified", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(loc));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    itemView.getContext().startActivity(mapIntent);
                } catch (Exception e) {
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(loc)));
                    itemView.getContext().startActivity(mapIntent);
                }
            });

            switchAlarm.setOnCheckedChangeListener(null);
            switchAlarm.setChecked(appointment.isAlarmSet());

            switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onAlarmToggle(appointment, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(appointment);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(appointment);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(appointment);
            });
        }
    }
}