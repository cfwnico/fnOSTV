package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
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
    private TextView countView;
    private TextView emptyView;
    private GridView gridView;

    public NativeFileBrowserView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(44), dp(30), dp(36), dp(28));
        view.setBackgroundColor(FnosTheme.COLOR_APP_BG);
        view.setVisibility(View.GONE);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        titleView = new TextView(context);
        titleView.setText("文件库");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(22);
        header.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        countView = new TextView(context);
        countView.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        countView.setTextSize(13);
        header.addView(countView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.addView(header, rowParams(0, 22));

        LinearLayout tools = new LinearLayout(context);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.addView(chip("筛选⌄"), chipParams());
        tools.addView(chip("添加日期⌄"), chipParams());
        tools.addView(chip("布局⌄"), chipParams());
        TextView hint = new TextView(context);
        hint.setText("菜单键收藏/取消收藏");
        hint.setTextColor(FnosTheme.COLOR_TEXT_DIM);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(0, dp(38), 1);
        hintParams.leftMargin = dp(12);
        tools.addView(hint, hintParams);
        view.addView(tools, rowParams(0, 22));

        gridView = new GridView(context);
        gridView.setAdapter(adapter);
        gridView.setFocusable(true);
        gridView.setNumColumns(GridView.AUTO_FIT);
        gridView.setColumnWidth(dp(160));
        gridView.setHorizontalSpacing(dp(18));
        gridView.setVerticalSpacing(dp(22));
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setCacheColorHint(Color.TRANSPARENT);
        gridView.setBackgroundColor(Color.TRANSPARENT);
        gridView.setSelector(FnosTheme.stroked(0x002F86F6, FnosTheme.COLOR_PRIMARY, 8, context));
        gridView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_MENU) {
                    int position = gridView.getSelectedItemPosition();
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
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View clicked, int position, long id) {
                listener.onFileEntrySelected(adapter.getItem(position));
            }
        });

        emptyView = new TextView(context);
        emptyView.setText("无数据");
        emptyView.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        emptyView.setTextSize(18);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        gridView.setEmptyView(emptyView);
        view.addView(gridView, new LinearLayout.LayoutParams(
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
        int count = entries == null ? 0 : entries.size();
        countView.setText("共 " + count + " 项");
        adapter.setEntries(entries, sortEntries);
        if (gridView != null) {
            gridView.requestFocus();
            if (count > 0) {
                gridView.setSelection(0);
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

    private TextView chip(String text) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextColor(FnosTheme.COLOR_TEXT);
        chip.setTextSize(13);
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_APP_BG, FnosTheme.COLOR_STROKE, 18, context));
        chip.setFocusable(true);
        return chip;
    }

    private LinearLayout.LayoutParams chipParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(86), dp(38));
        params.rightMargin = dp(12);
        return params;
    }

    private LinearLayout.LayoutParams rowParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
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
            LinearLayout card = convertView instanceof LinearLayout ? (LinearLayout) convertView : cardView();
            FnosFileEntry entry = getItem(position);
            TextView poster = (TextView) card.getChildAt(0);
            TextView title = (TextView) card.getChildAt(1);
            poster.setText(posterText(entry));
            title.setText(titleText(entry));
            return card;
        }

        private LinearLayout cardView() {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(0, 0, 0, 0);

            TextView poster = new TextView(context);
            poster.setTextColor(FnosTheme.COLOR_TEXT);
            poster.setTextSize(15);
            poster.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            poster.setPadding(dp(8), dp(8), dp(8), dp(12));
            poster.setBackgroundDrawable(FnosTheme.rounded(FnosTheme.COLOR_CARD, 8, context));
            card.addView(poster, new LinearLayout.LayoutParams(dp(154), dp(224)));

            TextView title = new TextView(context);
            title.setTextColor(FnosTheme.COLOR_TEXT);
            title.setTextSize(14);
            title.setGravity(Gravity.CENTER);
            title.setSingleLine(true);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(dp(154), ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = dp(8);
            card.addView(title, titleParams);
            return card;
        }

        private String posterText(FnosFileEntry entry) {
            if (entry.directory) {
                return "影\n\n媒体库";
            }
            String badge = entry.formatLabel();
            if (listener.isFileFavorite(entry)) {
                badge = "♡ " + badge;
            }
            return badge;
        }

        private String titleText(FnosFileEntry entry) {
            if (entry.name.length() <= 10) {
                return entry.name;
            }
            return entry.name.substring(0, 10) + "...";
        }
    }
}
