package org.example.quan_li_khach_san.network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Run three separate processes with different names. No Spring or database credentials needed. */
public final class DichVuSocketClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String name = args.length > 2 ? args[2] : "Phuc-vu-1";
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000); socket.setSoTimeout(5000);
            try (Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                 Reader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                out.write("SUB|" + ServiceProtocol.encode(ServiceProtocol.token()) + "|" + ServiceProtocol.encode(name) + "\n"); out.flush();
                if (!"READY".equals(ServiceProtocol.readLine(in))) throw new IOException("Server từ chối đăng ký.");
                socket.setSoTimeout(0);
                System.out.println("[" + name + "] Đã kết nối. Đợi yêu cầu dịch vụ; Ctrl+C để dừng.");
                Set<String> seen = new LinkedHashSet<>();
                String line;
                while ((line = ServiceProtocol.readLine(in)) != null) {
                    String[] fields = line.split("\\|", -1);
                    if (fields.length != 7 || !"SERVICE".equals(fields[0])) continue;
                    if (!seen.add(fields[1])) continue;
                    // Session-level deduplication only; retain at most 10,000 IDs.
                    if (seen.size() > 10000) seen.remove(seen.iterator().next());
                    System.out.printf("#%s | Lượt %s | Phòng %s | %s × %s | %s%n", fields[1], fields[2],
                            ServiceProtocol.decode(fields[3]), ServiceProtocol.decode(fields[4]), fields[5], fields[6]);
                }
                System.out.println("Server đã ngắt kết nối. Chạy lại client để kết nối lại.");
            }
        } catch (IOException | IllegalArgumentException ex) { System.err.println("Không thể nhận dịch vụ: " + ex.getMessage()); }
    }
}
