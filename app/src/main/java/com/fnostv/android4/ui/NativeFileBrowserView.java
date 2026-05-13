package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosRestClient;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class NativeFileBrowserView {
    public interface Listener {
        void onFileEntrySelected(FnosFileEntry entry);

        void onFileFavoriteToggled(FnosFileEntry entry);

        boolean isFileFavorite(FnosFileEntry entry);

        void onBrowserAction(String action);
    }

    private final Context context;
    private final Listener listener;
    private final EntryAdapter adapter = new EntryAdapter();
    private final PosterLoader posterLoader = new PosterLoader();
    private LinearLayout view;
    private TextView titleView;
    private TextView countView;
    private TextView emptyView;
    private GridView gridView;
    private String posterBaseUrl = "";

    public NativeFileBrowserView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setBackgroundColor(FnosTheme.COLOR_APP_BG);
        view.setVisibility(View.GONE);

        view.addView(sidebar(), new LinearLayout.LayoutParams(dp(250), ViewGroup.LayoutParams.MATCH_PARENT));
        view.addView(content(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return view;
    }

    public void show(String path, List<FnosFileEntry> entries) {
        showCustom("文件库", path == null || path.length() == 0 ? "根目录" : path, entries, true);
    }

    public void setPosterBaseUrl(String baseUrl) {
        posterBaseUrl = baseUrl == null ? "" : baseUrl;
        adapter.notifyDataSetChanged();
    }

    public void showCustom(String title, String subtitle, List<FnosFileEntry> entries, boolean sortEntries) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        titleView.setText(title == null || title.length() == 0 ? "文件库" : title);
        int count = entries == null ? 0 : entries.size();
        countView.setText("共 " + count + " 项" + subtitleText(subtitle));
        adapter.setEntries(entries, sortEntries);
        if (emptyView != null) {
            emptyView.setText(emptyText(title, subtitle));
        }
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

    private LinearLayout sidebar() {
        LinearLayout sidebar = new LinearLayout(context);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(dp(18), dp(26), dp(18), dp(20));
        sidebar.setBackgroundColor(FnosTheme.COLOR_SIDEBAR);

        LinearLayout brand = new LinearLayout(context);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(new FnosLogoMarkView(context), new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView logo = new TextView(context);
        logo.setText("fnOSTV");
        logo.setTextColor(FnosTheme.COLOR_TEXT);
        logo.setTextSize(18);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.setPadding(dp(8), 0, 0, 0);
        brand.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        sidebar.addView(brand, navParams(0, 28));

        sidebar.addView(navItem(FnosSidebarIconView.TYPE_HOME, "首页", NativeHomeView.ACTION_HOME), navParams(0, 8));
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_FAVORITE, "收藏", NativeHomeView.ACTION_FAVORITES), navParams(0, 30));
        sidebar.addView(section("媒体库"), navParams(0, 8));
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_LIBRARY, "影视大全", NativeHomeView.ACTION_MEDIA), navParams(0, 28));
        sidebar.addView(section("分类"), navParams(0, 8));
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_ALL, "全部", NativeHomeView.ACTION_ALL), navParams(0, 8));
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_MOVIE, "电影", NativeHomeView.ACTION_MOVIES), navParams(0, 8));
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_TV, "电视节目", NativeHomeView.ACTION_TV), navParams(0, 8));
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_OTHER, "其他", NativeHomeView.ACTION_OTHER), navParams(0, 8));
        return sidebar;
    }

    private LinearLayout content() {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(42), dp(30), dp(36), dp(28));
        content.setBackgroundColor(FnosTheme.COLOR_APP_BG);

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
        countView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(countView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(header, rowParams(0, 22));

        LinearLayout tools = new LinearLayout(context);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.addView(chip("筛选"), chipParams());
        tools.addView(chip("添加日期"), chipParams());
        tools.addView(chip("布局"), chipParams());
        TextView hint = new TextView(context);
        hint.setText("菜单键收藏 / 取消收藏");
        hint.setTextColor(FnosTheme.COLOR_TEXT_DIM);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(0, dp(38), 1);
        hintParams.leftMargin = dp(12);
        tools.addView(hint, hintParams);
        content.addView(tools, rowParams(0, 22));

        gridView = new GridView(context);
        gridView.setAdapter(adapter);
        gridView.setFocusable(true);
        gridView.setNumColumns(GridView.AUTO_FIT);
        gridView.setColumnWidth(dp(166));
        gridView.setHorizontalSpacing(dp(20));
        gridView.setVerticalSpacing(dp(24));
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
        emptyView.setText("暂无内容");
        emptyView.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        emptyView.setTextSize(18);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        gridView.setEmptyView(emptyView);
        content.addView(gridView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));
        content.addView(emptyView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));
        return content;
    }

    private View navItem(String iconType, String label, final String action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(10), 0);
        row.setClickable(true);
        FocusStyler.apply(row, FnosTheme.COLOR_SIDEBAR, FnosTheme.COLOR_CARD, 6);

        FnosSidebarIconView iconView = new FnosSidebarIconView(context, iconType);
        row.addView(iconView, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(FnosTheme.COLOR_TEXT);
        text.setTextSize(15);
        text.setPadding(dp(6), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onBrowserAction(action);
            }
        });
        return row;
    }

    private TextView section(String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        view.setTextSize(13);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(4), 0, 0, 0);
        return view;
    }

    private TextView chip(String text) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextColor(FnosTheme.COLOR_TEXT);
        chip.setTextSize(13);
        chip.setGravity(Gravity.CENTER);
        FocusStyler.apply(chip, FnosTheme.COLOR_APP_BG, FnosTheme.COLOR_CARD, 18);
        return chip;
    }

    private String subtitleText(String subtitle) {
        if (subtitle == null || subtitle.length() == 0) {
            return "";
        }
        return "  |  " + subtitle;
    }

    private String emptyText(String title, String subtitle) {
        String value = subtitle == null || subtitle.length() == 0 ? "暂无内容" : subtitle;
        String prefix = title == null || title.length() == 0 ? "" : title + "\n";
        return prefix + value;
    }

    private LinearLayout.LayoutParams chipParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(86), dp(38));
        params.rightMargin = dp(12);
        return params;
    }

    private LinearLayout.LayoutParams navParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40));
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
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
            FrameLayout poster = (FrameLayout) card.getChildAt(0);
            ImageView image = (ImageView) poster.getChildAt(0);
            TextView fallback = (TextView) poster.getChildAt(1);
            TextView title = (TextView) card.getChildAt(1);
            fallback.setText(posterText(entry));
            posterLoader.load(posterBaseUrl, entry, poster, image, fallback);
            title.setText(titleText(entry));
            card.setContentDescription(entry.name);
            return card;
        }

        private LinearLayout cardView() {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(0, 0, 0, 0);
            card.setFocusable(false);

            FrameLayout poster = new FrameLayout(context);
            poster.setBackgroundDrawable(FnosTheme.rounded(FnosTheme.COLOR_CARD, 8, context));
            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setVisibility(View.GONE);
            poster.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            TextView fallback = new TextView(context);
            fallback.setTextColor(FnosTheme.COLOR_TEXT);
            fallback.setTextSize(15);
            fallback.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            fallback.setPadding(dp(8), dp(8), dp(8), dp(12));
            poster.addView(fallback, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            card.addView(poster, new LinearLayout.LayoutParams(dp(160), dp(226)));

            TextView title = new TextView(context);
            title.setTextColor(FnosTheme.COLOR_TEXT);
            title.setTextSize(14);
            title.setGravity(Gravity.CENTER);
            title.setSingleLine(true);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(dp(160), ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = dp(8);
            card.addView(title, titleParams);
            return card;
        }

        private String posterText(FnosFileEntry entry) {
            String badge;
            if (entry.directory) {
                badge = "目录";
            } else {
                badge = entry.formatLabel();
            }
            if (listener.isFileFavorite(entry)) {
                badge = "* " + badge;
            }
            if (entry.posterPath.length() > 0) {
                return "海报\n\n" + badge;
            }
            if (entry.directory) {
                return "影视\n\n" + badge;
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

    private final class PosterLoader {
        private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(12);

        void load(String baseUrl, FnosFileEntry entry, FrameLayout frame, ImageView image, TextView fallback) {
            String url = FnosRestClient.posterImageUrl(baseUrl, entry.posterPath, 400);
            frame.setTag(url);
            image.setImageDrawable(null);
            image.setVisibility(View.GONE);
            fallback.setVisibility(View.VISIBLE);
            if (url.length() == 0) {
                return;
            }
            Bitmap cached = cache.get(url);
            if (cached != null) {
                image.setImageBitmap(cached);
                image.setVisibility(View.VISIBLE);
                fallback.setVisibility(View.GONE);
                return;
            }
            new PosterTask(url, frame, image, fallback).execute();
        }

        private final class PosterTask extends AsyncTask<Void, Void, Bitmap> {
            private final String url;
            private final FrameLayout frame;
            private final ImageView image;
            private final TextView fallback;

            PosterTask(String url, FrameLayout frame, ImageView image, TextView fallback) {
                this.url = url;
                this.frame = frame;
                this.image = image;
                this.fallback = fallback;
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                HttpURLConnection connection = null;
                InputStream stream = null;
                try {
                    connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(8000);
                    connection.setRequestProperty("Accept", "image/*");
                    if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                        return null;
                    }
                    stream = connection.getInputStream();
                    return BitmapFactory.decodeStream(stream);
                } catch (RuntimeException ignored) {
                    return null;
                } catch (Exception ignored) {
                    return null;
                } finally {
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (Exception ignored) {
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null || !url.equals(frame.getTag())) {
                    return;
                }
                cache.put(url, bitmap);
                image.setImageBitmap(bitmap);
                image.setVisibility(View.VISIBLE);
                fallback.setVisibility(View.GONE);
            }
        }
    }
}
