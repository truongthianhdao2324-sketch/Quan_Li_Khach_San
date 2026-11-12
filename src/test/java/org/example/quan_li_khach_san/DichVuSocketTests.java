package org.example.quan_li_khach_san;

import org.example.quan_li_khach_san.network.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DichVuSocketTests {
    static final String TOKEN = "socket-test";
    static class Client implements AutoCloseable {
        Socket socket = new Socket(); Reader in; Writer out;
        Client(int port) throws IOException {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 2000); socket.setSoTimeout(10000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        }
        void send(String text) throws IOException { out.write(text + "\n"); out.flush(); }
        String read() throws IOException { return ServiceProtocol.readLine(in); }
        public void close() throws IOException { socket.close(); }
    }
    @Test void threeRealSubscribersReceiveEveryConcurrentMessageAndReconnectWorks() throws Exception {
        try (DichVuSocketServer server = new DichVuSocketServer("127.0.0.1", 0, TOKEN)) {
            ExecutorService pool = Executors.newFixedThreadPool(12);
            Future<?> serving = pool.submit(() -> { try { server.serve(); } catch (IOException ex) { throw new UncheckedIOException(ex); } });
            List<Client> clients = new ArrayList<>();
            try {
                for (int i = 0; i < 3; i++) { Client client = new Client(server.getPort()); clients.add(client); client.send("SUB|" + ServiceProtocol.encode(TOKEN) + "|" + ServiceProtocol.encode("Bộ phận " + i)); assertEquals("READY", client.read()); }
                assertEquals(3, server.getSubscriberCount());
                List<Future<Set<String>>> received = new ArrayList<>();
                for (Client client : clients) received.add(pool.submit(() -> {
                    Set<String> ids = new HashSet<>(); for (int i = 0; i < 24; i++) {
                        String[] fields = client.read().split("\\|", -1); assertEquals(7, fields.length); assertEquals("SERVICE", fields[0]);
                        assertEquals("Nước suối | trà\nTiếng Việt", ServiceProtocol.decode(fields[4])); assertTrue(ids.add(fields[1]));
                    } return ids;
                }));
                CountDownLatch ready = new CountDownLatch(6), start = new CountDownLatch(1);
                List<Future<?>> publishers = new ArrayList<>();
                for (int worker = 0; worker < 6; worker++) { final int index = worker; publishers.add(pool.submit(() -> {
                    ready.countDown(); assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int message = 1; message <= 4; message++) try (Client sender = new Client(server.getPort())) {
                        int id = index * 4 + message;
                        sender.send("EVENT|" + ServiceProtocol.encode(TOKEN) + "|" + id + "|21|" + ServiceProtocol.encode("202") + "|" + ServiceProtocol.encode("Nước suối | trà\nTiếng Việt") + "|2|2026-11-04T12:00|TEST");
                        assertEquals("ACK|" + id + "|3", sender.read());
                    } return null;
                })); }
                assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
                for (Future<?> sender : publishers) sender.get(15, TimeUnit.SECONDS);
                Set<String> expected = new HashSet<>(); for (int i = 1; i <= 24; i++) expected.add(Integer.toString(i));
                for (Future<Set<String>> result : received) assertEquals(expected, result.get(15, TimeUnit.SECONDS));
                clients.get(0).close();
                try (Client next = new Client(server.getPort())) { next.send("SUB|" + ServiceProtocol.encode(TOKEN) + "|" + ServiceProtocol.encode("Nối lại")); assertEquals("READY", next.read()); next.send("PING"); assertEquals("PONG", next.read()); next.send("UNKNOWN"); assertEquals("ERROR|COMMAND", next.read()); }
                try (Client malformed = new Client(server.getPort())) { malformed.send("BROKEN|" + ServiceProtocol.encode(TOKEN)); assertEquals("ERROR|FORMAT", malformed.read()); }
                try (Client unauthorized = new Client(server.getPort())) { unauthorized.send("SUB|wrong|name"); assertEquals("ERROR|AUTH", unauthorized.read()); }
            } finally {
                for (Client client : clients) client.close(); server.close(); serving.get(5, TimeUnit.SECONDS); pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }
    @Test void shutdownClosesAnIdleSubscriberAndStopsAccepting() throws Exception {
        try (DichVuSocketServer server = new DichVuSocketServer("127.0.0.1", 0, TOKEN)) {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            Future<?> serving = pool.submit(() -> { try { server.serve(); } catch (IOException ex) { throw new UncheckedIOException(ex); } });
            try (Client idle = new Client(server.getPort())) {
                idle.send("SUB|" + ServiceProtocol.encode(TOKEN) + "|" + ServiceProtocol.encode("Idle")); assertEquals("READY", idle.read());
                server.close(); assertNull(idle.read()); serving.get(3, TimeUnit.SECONDS); assertEquals(0, server.getSubscriberCount());
            } finally { server.close(); pool.shutdownNow(); assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS)); }
        }
    }
    @Test void protocolRejectsPartialAndOversizedFrames() throws IOException {
        assertThrows(EOFException.class, () -> ServiceProtocol.readLine(new StringReader("unfinished")));
        assertThrows(IOException.class, () -> ServiceProtocol.readLine(new StringReader("x".repeat(8193) + "\n")));
        assertThrows(IOException.class, () -> ServiceProtocol.readLine(new StringReader("\r".repeat(8193) + "\n")));
        assertThrows(IOException.class, () -> ServiceProtocol.readLine(new StringReader("PI\rNG\n")));
        assertEquals("PING", ServiceProtocol.readLine(new StringReader("PING\r\n")));
    }
}
