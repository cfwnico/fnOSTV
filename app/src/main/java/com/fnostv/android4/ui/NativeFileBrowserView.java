package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class NativeFileBrowserView {
    public interface Listener {
        void onFileEntrySelected(FnosFileEntry entry);
    }

    private final Context context;
    private final Listener listener;
    private final EntryAdapter adapter = new EntryAdapter();
    private LinearLayout view;
    private TextView pathView;
    private ListView listView;

    public NativeFileBrowserView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(36), dp(28), dp(36), dp(28));
        view.setBackgroundColor(0xFF101820);
        view.setVisibility(View.GONE);

        TextView title = new TextView(context);
        title.setText("文件库");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        view.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        pathView = new TextView(context);
        pathView.setTextColor(0xFFB8C7D9);
        pathView.setTextSize(14);
        pathView.setSingleLine(true);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        pathParams.topMargin = dp(6);
        pathParams.bottomMargin = dp(18);
        view.addView(pathView, pathParams);

        listView = new ListView(context);
        listView.setAdapter(adapter);
        listView.setFocusable(true);
        listView.setDividerHeight(1);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setBackgroundColor(Color.TRANSPARENT);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View clicked, int position, long id) {
                listener.onFileEntrySelected(adapter.getItem(position));
            }
        });
        view.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));
        return view;
    }

    public void show(String path, List<FnosFileEntry> entries) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        pathView.setText(path == null || path.length() == 0 ? "根目录" : path);
        adapter.setEntries(entries);
        if (listView != null) {
            listView.requestFocus();
            if (entries != null && entries.size() > 0) {
                listView.setSelection(0);
            }
        }
    }

    public void hide() {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public boolean isVisible() {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private final class EntryAdapter extends BaseAdapter {
        private final List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();

        void setEntries(List<FnosFileEntry> values) {
            entries.clear();
            if (values != null) {
                entries.addAll(values);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public FnosFileEntry getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView row = convertView instanceof TextView ? (TextView) convertView : rowView();
            FnosFileEntry entry = getItem(position);
            row.setText((entry.directory ? "[目录] " : "[文件] ") + entry.name);
            return row;
        }

        private TextView rowView() {
            TextView row = new TextView(context);
            row.setTextColor(Color.WHITE);
            row.setTextSize(18);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(18), 0, dp(18), 0);
            row.setMinHeight(dp(54));
            row.setBackgroundColor(0xFF182536);
            return row;
        }
    }
}
