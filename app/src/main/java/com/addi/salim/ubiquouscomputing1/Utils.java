package com.addi.salim.ubiquouscomputing1;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static void saveDataToFile(Context contextApp, String fileName, List<Event> events) {
        final FileOutputStream outputStream;
        final byte[] data = new byte[events.size() * (3 * Double.BYTES + Long.BYTES)];
        int currentByteIndex = 0;
        for (int i = 0; i < events.size(); i++) {
            final Event event = events.get(i);
            ByteBuffer.wrap(data).putLong(currentByteIndex, event.time);
            currentByteIndex += Long.BYTES;
            ByteBuffer.wrap(data).putDouble(currentByteIndex, event.x);
            currentByteIndex += Double.BYTES;
            ByteBuffer.wrap(data).putDouble(currentByteIndex, event.y);
            currentByteIndex += Double.BYTES;
            ByteBuffer.wrap(data).putDouble(currentByteIndex, event.z);
            currentByteIndex += Double.BYTES;
        }

        try {
            outputStream = contextApp.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Event> readDataFromFile(Context contextApp, String fileName) {
        final File file = new File(contextApp.getFilesDir(), fileName);
        int size = (int) file.length();
        final byte[] data = new byte[size];
        try {
            final InputStream inputStream = contextApp.openFileInput(fileName);
            final BufferedInputStream buffer = new BufferedInputStream(inputStream);
            buffer.read(data, 0, size);
            buffer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final List<Event> events = new ArrayList<>();
        for (int i = 0; i < size; ) {
            final long time = ByteBuffer.wrap(data).getLong(i);
            i += Long.BYTES;
            final double x = ByteBuffer.wrap(data).getDouble(i);
            i += Double.BYTES;
            final double y = ByteBuffer.wrap(data).getDouble(i);
            i += Double.BYTES;
            final double z = ByteBuffer.wrap(data).getDouble(i);
            i += Double.BYTES;
            events.add(new Event(time, x, y, z));
        }

        return events;
    }
}
