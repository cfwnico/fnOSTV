package com.fnostv.android4.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosRestClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class PosterLoader {
    private static final int MAX_DECODE_WIDTH = 720;
    private static final int MAX_DECODE_HEIGHT = 720;
    private static final int MAX_ACTIVE_LOADS = 3;
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(18);
    private final PosterLoadThrottle throttle = new PosterLoadThrottle(MAX_ACTIVE_LOADS);
    private String authorizationToken = "";

    void setAuthorizationToken(String token) {
        authorizationToken = token == null ? "" : token;
    }

    void load(String baseUrl, FnosFileEntry entry, FrameLayout frame, ImageView image, TextView fallback) {
        load(baseUrl, entry, frame, image, fallback, null);
    }

    void load(String baseUrl, FnosFileEntry entry, FrameLayout frame, ImageView image, TextView fallback, View loadedOverlay) {
        String url = entry == null ? "" : FnosRestClient.posterImageUrl(baseUrl, entry.posterPath, 400);
        frame.setTag(url);
        image.setImageDrawable(null);
        image.setVisibility(View.GONE);
        fallback.setVisibility(View.VISIBLE);
        if (loadedOverlay != null) {
            loadedOverlay.setVisibility(View.GONE);
        }
        if (url.length() == 0) {
            return;
        }
        Bitmap cached = cache.get(url);
        if (cached != null) {
            image.setImageBitmap(cached);
            image.setVisibility(View.VISIBLE);
            fallback.setVisibility(View.GONE);
            if (loadedOverlay != null) {
                loadedOverlay.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (!throttle.tryStart(url)) {
            return;
        }
        new PosterTask(url, authorizationToken, frame, image, fallback, loadedOverlay).execute();
    }

    private final class PosterTask extends AsyncTask<Void, Void, Bitmap> {
        private final String url;
        private final String authorizationToken;
        private final FrameLayout frame;
        private final ImageView image;
        private final TextView fallback;
        private final View loadedOverlay;

        PosterTask(String url, String authorizationToken, FrameLayout frame, ImageView image, TextView fallback, View loadedOverlay) {
            this.url = url;
            this.authorizationToken = authorizationToken == null ? "" : authorizationToken;
            this.frame = frame;
            this.image = image;
            this.fallback = fallback;
            this.loadedOverlay = loadedOverlay;
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
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null && cookie.length() > 0) {
                    connection.setRequestProperty("Cookie", cookie);
                }
                if (authorizationToken.length() > 0) {
                    connection.setRequestProperty("Authorization", authorizationToken);
                }
                if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                    return null;
                }
                stream = connection.getInputStream();
                byte[] bytes = readAllBytes(stream);
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = BitmapSampleSize.forBounds(
                        bounds.outWidth,
                        bounds.outHeight,
                        MAX_DECODE_WIDTH,
                        MAX_DECODE_HEIGHT);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
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

        private byte[] readAllBytes(InputStream stream) throws Exception {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            throttle.finish(url);
            if (bitmap == null || !url.equals(frame.getTag())) {
                return;
            }
            cache.put(url, bitmap);
            image.setImageBitmap(bitmap);
            image.setVisibility(View.VISIBLE);
            fallback.setVisibility(View.GONE);
            if (loadedOverlay != null) {
                loadedOverlay.setVisibility(View.VISIBLE);
            }
        }
    }
}
