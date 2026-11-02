package com.shuimo0413.yosuganosora.hdremake;

import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies the APK's dialogue portrait change to newly imported or existing data. */
public final class DialoguePortraitPatch {
    private static final int MAX_SCRIPT_BYTES = 2 * 1024 * 1024;
    private static final Pattern PORTRAIT_METHOD = Pattern.compile(
        "(?ms)^([\\t ]*)function[\\t ]+layoutFaceImage[\\t ]*"
        + "\\([\\t ]*face[\\t ]*\\)[\\t ]*\\{\\r?\\n.*?^\\1\\}[\\t ]*"
        + "(?=\\r?\\n[\\t ]*\\r?\\n[\\t ]*function[\\t ]+layoutRoleNameImage[\\t ]*\\()"
    );

    private DialoguePortraitPatch() {}

    public static void apply(File dataDir) throws IOException {
        if (dataDir == null) {
            throw new IOException("Game data directory is unavailable");
        }
        AtomicFile script = new AtomicFile(new File(dataDir, "system/MessageFrame.tjs"));
        byte[] original = readScript(script);
        byte[] patched = patchSource(original);
        if (Arrays.equals(original, patched)) return;

        FileOutputStream output = script.startWrite();
        try {
            output.write(patched);
            script.finishWrite(output);
        } catch (Throwable error) {
            script.failWrite(output);
            throw error;
        }
    }

    private static byte[] readScript(AtomicFile script) throws IOException {
        try (FileInputStream input = script.openRead();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                if (output.size() + length > MAX_SCRIPT_BYTES) {
                    throw new IOException("MessageFrame.tjs exceeds the supported script size");
                }
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }

    static byte[] patchSource(byte[] original) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        if (original.length >= 2) {
            if ((original[0] & 255) == 255 && (original[1] & 255) == 254) {
                charset = StandardCharsets.UTF_16LE;
            } else if ((original[0] & 255) == 254 && (original[1] & 255) == 255) {
                charset = StandardCharsets.UTF_16BE;
            }
        }
        // Explicit endianness keeps the original BOM as a character, so encoding
        // the result retains the BOM byte for byte (including a UTF-8 BOM).
        String source = new String(original, charset);
        if (!Arrays.equals(original, source.getBytes(charset))) {
            throw new IOException("MessageFrame.tjs has an unsupported text encoding");
        }

        Matcher method = PORTRAIT_METHOD.matcher(source);
        if (!method.find()) {
            throw new IOException("MessageFrame.tjs portrait method was not found");
        }
        int start = method.start();
        int end = method.end();
        String indent = method.group(1);
        if (method.find()) {
            throw new IOException("MessageFrame.tjs contains more than one portrait method");
        }
        String newline = source.indexOf("\r\n", start) >= 0 ? "\r\n" : "\n";
        String replacement = indent + "function layoutFaceImage(face){" + newline
            + indent + "\t_imgFace.visible = false;" + newline
            + indent + "}";
        String patched = source.substring(0, start) + replacement + source.substring(end);
        return patched.getBytes(charset);
    }
}
