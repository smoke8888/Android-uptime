package com.smoke.uptime;

import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AppSelectActivity extends ListActivity {

    private PackageManager pm;
    private List infos;
    //______________________________________________________________________________________________
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appselect);

        pm = getPackageManager();
        infos = checkForLaunchIntent(pm.getInstalledApplications(PackageManager.GET_META_DATA));

        AppAdapter appListAdapter = new AppAdapter(this, R.layout.app_list_view, pm, infos);
        setListAdapter(appListAdapter);
    }

    private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
        ArrayList appList = new ArrayList();

        for(ApplicationInfo info : list) {
            try{
                if(pm.getLaunchIntentForPackage(info.packageName) != null) {
                    appList.add(info);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        return appList;
    }

    //______________________________________________________________________________________________
    private class AppAdapter extends ArrayAdapter<ApplicationInfo> {

        private List<ApplicationInfo> infos;
        private LayoutInflater inflater;
        private PackageManager pm;
        private Context context;

        AppAdapter(Context context, int textViewResourceId, PackageManager pctmng, List<ApplicationInfo> infos) {
            super(context, textViewResourceId);
            this.context = context;
            this.infos = infos;
            this.pm = pctmng;

        }


        @Override
        public int getCount() {
            return ((null != infos) ? infos.size() : 0);
        }

        @Override
        public ApplicationInfo getItem(int position) {
            return ((null != infos) ? infos.get(position) : null);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            View view = convertView;

            if (view == null) {
                inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.app_list_view, parent, false);
            }

            ApplicationInfo data = infos.get(position);

            if (data != null) {
                TextView app_name = (TextView) view.findViewById(R.id.app_name);
                ImageView app_icon = (ImageView) view.findViewById(R.id.app_icon);
                app_name.setText(infos.get(position).loadLabel(pm).toString());
                app_icon.setImageDrawable(infos.get(position).loadIcon(pm));
            }
            //inflater = getLayoutInflater();
            //View row = inflater.inflate(android.R.layout.activity_list_item, parent, false);
            return view;
        }
    }
}
