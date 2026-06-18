package com.zipflash.mrp.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.zipflash.mrp.models.AppInfo;
import com.zipflash.mrp.R; // make sure this points to your app R, not androidx.appcompat.R

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder> {

    private Context context;
    private List<AppInfo> appList;       // Full list of apps
    private List<AppInfo> filteredList;  // Filtered list for search

    public AppsAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
        this.filteredList = new ArrayList<>(appList); // copy for filtering
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final AppInfo app = filteredList.get(position);

        // If it's an activity, show name + suffix
        if (app.isActivity()) {
            holder.appName.setText(app.getName() + " · Activity");
        } else {
            holder.appName.setText(app.getName());
        }

        // Set icon
        holder.appIcon.setImageDrawable(app.getIcon());

        // Launch app or activity when clicked
        holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (app.isActivity()) {
                            // Launch specific activity
                            Intent intent = new Intent();
                            intent.setClassName(app.getPackageName(), app.getActivityName());
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } else {
                            // Launch default app
                            Intent launchIntent = context.getPackageManager()
                                .getLaunchIntentForPackage(app.getPackageName());
                            if (launchIntent != null) {
                                context.startActivity(launchIntent);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // 🔍 Filter apps by name
    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(appList);
        } else {
            for (AppInfo app : appList) {
                if (app.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    // 🔄 Update list when reloading apps
    public void updateData(List<AppInfo> newList) {
        appList.clear();
        appList.addAll(newList);

        filteredList.clear();
        filteredList.addAll(newList);

        notifyDataSetChanged();
    }

    // 📌 ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
        }
    }
}
