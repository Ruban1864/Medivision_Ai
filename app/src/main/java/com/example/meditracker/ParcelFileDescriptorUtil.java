// ParcelFileDescriptorUtil.java
package com.example.meditracker;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ParcelFileDescriptorUtil {
    public static ParcelFileDescriptor getFileDescriptor(Context context, Uri uri) throws Exception {
        InputStream input = context.getContentResolver().openInputStream(uri);
        File file = File.createTempFile("temp_pdf", ".pdf", context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
