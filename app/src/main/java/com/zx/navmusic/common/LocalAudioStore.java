package com.zx.navmusic.common;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import cn.hutool.core.io.IoUtil;

public class LocalAudioStore {

    public static Uri find(Context ctx, String name) {
        name = name.replace("/", "_");
        Uri collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
        };
        ContentResolver resolver = ctx.getContentResolver();
        String selection = MediaStore.Audio.Media.DISPLAY_NAME + " = ?";
        String[] selectionArgs = new String[] {name};
        try (Cursor cursor = resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

            while (cursor.moveToNext()) {
                // Get values of columns for a given Audio.
                long id = cursor.getLong(idColumn);
                int size = cursor.getInt(sizeColumn);

                App.log("[本地存储]读取: [{}], size: [{}]", name, size);

                return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            }
        }
        return null;
    }

    public static void put(Activity activity, String name, InputStream in) {
        put(activity, name, out -> {
            IoUtil.copy(in, out);
        });
    }

    public static void put(Activity activity, String name, Consumer<OutputStream> action) {
        ContentResolver resolver = activity.getContentResolver();

        // Find all audio files on the primary external storage device.
        Uri audioCollection = MediaStore.Audio.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // Publish a new song.
        ContentValues newSongDetails = new ContentValues();
        newSongDetails.put(MediaStore.Audio.Media.DISPLAY_NAME, name);
        newSongDetails.put(MediaStore.Audio.Media.IS_PENDING, 1);

        // Keep a handle to the new song's URI in case you need to modify it
        // later.
        Uri myFavoriteSongUri = resolver.insert(audioCollection, newSongDetails);
        App.log("[本地存储][{}]写入: {}", name, myFavoriteSongUri.toString());

        // "w" for write.
        try (ParcelFileDescriptor pfd =
                     resolver.openFileDescriptor(myFavoriteSongUri, "w", null)) {
            // Write data into the pending audio file.
            if (pfd != null) {
                OutputStream out = new FileOutputStream(pfd.getFileDescriptor());
                action.accept(out);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Now that you're finished, release the "pending" status and let other apps
        // play the audio track.
        newSongDetails.clear();
        newSongDetails.put(MediaStore.Audio.Media.IS_PENDING, 0);
        resolver.update(myFavoriteSongUri, newSongDetails, null, null);
    }

    public static void read(Activity activity, Uri uri, Consumer<InputStream> consumer) {
        ContentResolver resolver = activity.getContentResolver();
        String readOnlyMode = "r";
        try (ParcelFileDescriptor pfd =
                     resolver.openFileDescriptor(uri, readOnlyMode)) {
            // Perform operations on "pfd".
            FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
            consumer.accept(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
