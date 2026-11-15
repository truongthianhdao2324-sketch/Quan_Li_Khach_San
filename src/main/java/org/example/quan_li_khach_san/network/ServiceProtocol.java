package org.example.quan_li_khach_san.network;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** One UTF-8 line per message. Text fields use URL-safe Base64, never raw separators. */
public final class ServiceProtocol {
    public static final int MAX_LINE = 8192;
    private ServiceProtocol() { }
    public static String encode(String text) { return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8)); }
    public static String decode(String text) { return new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8); }
    public static String readLine(Reader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '\n') return line.toString();
            if (c == '\r') {
                if (reader.read() != '\n') throw new IOException("Dấu CR chỉ hợp lệ ngay trước LF.");
                return line.toString();
            }
            line.append((char)c);
            if (line.length() > MAX_LINE) throw new IOException("Thông điệp vượt quá giới hạn 8192 ký tự.");
        }
        if (!line.isEmpty()) throw new EOFException("Thông điệp bị ngắt giữa dòng.");
        return null;
    }
    public static String token() { return System.getenv().getOrDefault("QLKS_SOCKET_TOKEN", "demo-noi-bo"); }
}
