package com.github.yeriomin.yalpstore;

import android.content.Context;
import android.util.Log;

import com.github.yeriomin.yalpstore.model.App;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;

public class DeltaPatcherFactory {

    static public DeltaPatcherAbstract get(Context context, App app) {
        File patch = Paths.getDeltaPath(context, app.getPackageName(), app.getVersionCode());
        if (isGZipped(patch)) {
            return new DeltaPatcherGDiffGzipped(context, app);
        } else {
            return new DeltaPatcherGDiff(context, app);
        }
    }

    static private boolean isGZipped(File f) {
        int magic = 0;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "r");
            magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
        } catch (IOException e) {
            Log.e(DeltaPatcherGDiff.class.getSimpleName(), "Could not check if patch is gzipped");
        } finally {
            Util.closeSilently(raf);
        }
        return magic == GZIPInputStream.GZIP_MAGIC;
    }
}
