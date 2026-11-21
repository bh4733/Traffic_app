package com.example.trafficapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    public interface OnSituationClickListener {
        void onSituationClick(Situation s);
    }

    private List<Situation> mDataSet = new ArrayList<>();
    private final OnSituationClickListener listener;
    private boolean showDistance = false;
    private double originLat;
    private double originLon;

    public SituationAdapter(OnSituationClickListener listener) {
        this.listener = listener;
    }

    public void setOrigin(boolean show, double lat, double lon) {
        showDistance = show;
        originLat = lat;
        originLon = lon;
    }

    public void setData(List<Situation> data) {
        mDataSet = data;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final View delayStrip;
        private final ImageView typeIcon;
        private final TextView typeText;
        private final TextView commentText;

        public ViewHolder(View v) {
            super(v);
            delayStrip = v.findViewById(R.id.delayStrip);
            typeIcon = v.findViewById(R.id.typeIcon);
            typeText = v.findViewById(R.id.typeText);
            commentText = v.findViewById(R.id.commentText);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.situation, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Situation s = mDataSet.get(position);

        String title = s.getDisplayType() + " · " + s.getRoadName();
        if (showDistance && s.hasLocation()) {
            title += String.format(java.util.Locale.UK, " · %.1f km", s.distanceKmFrom(originLat, originLon));
        }
        holder.typeText.setText(title);
        holder.commentText.setText(s.getComment() != null ? s.getComment() : "No description");

        holder.typeIcon.setImageResource(SituationStyle.iconFor(s));
        holder.delayStrip.setBackgroundColor(holder.itemView.getContext().getResources()
                .getColor(SituationStyle.delayColourRes(s), null));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSituationClick(s);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}
