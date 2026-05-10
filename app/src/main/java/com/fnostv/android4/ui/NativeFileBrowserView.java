package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class NativeFileBrowserView {
    public interface Listener {
        void onFileEntrySelected(FnosFileEntry entry);

        void onFileFavoriteToggled(FnosFileEntry entry);

        boolean isFileFavorite(FnosFileEntry entry);
    }

    private final Context context;
    private final Listener listener;
    private final EntryAdapter adapter = new EntryAdapter();
    private LinearLayout view;
    private TextView titleView;
    private TextView pathView;
    private TextView emptyView;
    private ListView listView;

    public NativeFileBrowserView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(36), dp(28), dp(36), dp(28));
        view.setBackgroundColor(FnosTheme.COLOR_APP_BG);
        view.setVisibility(View.GONE);

        titleView = new TextView(context);
        titleView.setText("文件库");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(26);
        view.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        pathView = new TextView(context);
        pathView.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
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
        listView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int position = listView.getSelectedItemPosition();
                    if (position >= 0 && position < adapter.getCount()) {
                        FnosFileEntry entry = adapter.getItem(position);
                        if (!entry.directory) {
                            listener.onFileFavoriteToggled(entry);
                            adapter.notifyDataSetChanged();
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View clicked, int position, long id) {
                listener.onFileEntrySelected(adapter.getItem(position));
            }
        });
        emptyView = new TextView(context);
        emptyView.setText("暂无内容");
        emptyView.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        emptyView.setTextSize(18);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        listView.setEmptyView(emptyView);
        view.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));
        view.addView(emptyView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));
        return view;
    }

    public void show(String path, List<FnosFileEntry> entries) {
        showCustom("文件库", path == null || path.length() == 0 ? "根目录" : path, entries, true);
    }

    public void showCustom(String title, String subtitle, List<FnosFileEntry> entries, boolean sortEntries) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        titleView.setText(title == null || title.length() == 0 ? "文件库" : title);
        String path = subtitle == null || subtitle.length() == 0 ? "根目录" : subtitle;
        pathView.setText(path + "    右键收藏/取消收藏");
        adapter.setEntries(entries, sortEntries);
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
        return FnosTheme.dp(context, value);
    }

    private final class EntryAdapter extends BaseAdapter {
        private final List<FnosFileEntry> entries = new ArrayList<FnosFileEntry>();

        void setEntries(List<FnosFileEntry> values, boolean sortEntries) {
            entries.clear();
            if (values != null) {
                entries.addAll(values);
                if (sortEntries) {
                    Collections.sort(entries, new Comparator<FnosFileEntry>() {
                        @Override
                        public int compare(FnosFileEntry left, FnosFileEntry right) {
                            if (left.directory != right.directory) {
                                return left.directory ? -1 : 1;
                            }
                            if (left.isVideo() != right.isVideo()) {
                                return left.isVideo() ? -1 : 1;
                            }
                            return left.name.compareToIgnoreCase(right.name);
                        }
                    });
                }
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
            row.setText(labelFor(entry));
            return row;
        }

        private String labelFor(FnosFileEntry entry) {
            if (entry.directory) {
                return "[目录] " + entry.name;
            }
            String favorite = listener.isFileFavorite(entry) ? "[收藏] " : "";
            String prefix = entry.isVideo() ? "[视频] " : "[文件] ";
            String size = formatSize(entry.size);
            return size.length() == 0 ? favorite + prefix + entry.name : favorite + prefix + entry.name + "    " + size;
        }

        private String formatSize(long size) {
            if (size <= 0L) {
                return "";
            }
            if (size >= 1024L * 1024L * 1024L) {
                return String.format("%.1f GB", size / 1024.0 / 1024.0 / 1024.0);
            }
            if (size >= 1024L * 1024L) {
                return String.format("%.1f MB", size / 1024.0 / 1024.0);
            }
            if (size >= 1024L) {
                return String.format("%.1f KB", size / 1024.0);
            }
            return size + " B";
        }

        private TextView rowView() {
            TextView row = new TextView(context);
            row.setTextColor(Color.WHITE);
            row.setTextSize(18);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(18), 0, dp(18), 0);
            row.setMinHeight(dp(54));
            row.setBackgroundDrawable(FnosTheme.rounded(FnosTheme.COLOR_CARD, 4, context));
            return row;
        }
    }
}
