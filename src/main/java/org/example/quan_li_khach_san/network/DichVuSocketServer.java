package org.example.quan_li_khach_san.network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent Java program. Based on the Runnable-per-connection example in chapter 5. */
public final class DichVuSocketServer implements AutoCloseable {
    private final ServerSocket listener;
    private final String token;
    private final Set<Handler> connections = ConcurrentHashMap.newKeySet();
    private final Set<Handler> subscribers = ConcurrentHashMap.newKeySet();
    private final Semaphore capacity = new Semaphore(64);
    private final AtomicInteger sequence = new AtomicInteger();
    private final Object lifecycle = new Object();
    private final int deliveryTimeoutMillis;
    private volatile boolean running = true;
    public DichVuSocketServer(String host, int port, String token) throws IOException {
        this(host, port, token, 1500);
    }
    public DichVuSocketServer(String host, int port, String token, int deliveryTimeoutMillis) throws IOException {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Token không được để trống.");
        if (deliveryTimeoutMillis < 1) throw new IllegalArgumentException("Thời hạn gửi phải dương.");
        this.token = token;
        this.deliveryTimeoutMillis = deliveryTimeoutMillis;
        listener = new ServerSocket(); listener.setReuseAddress(true);
        listener.bind(new InetSocketAddress(host, port));
    }
    public int getPort() { return listener.getLocalPort(); }
    public int getSubscriberCount() { return subscribers.size(); }
    public void serve() throws IOException {
        log("Lắng nghe " + listener.getLocalSocketAddress());
        while (running) {
            try {
                Socket socket = listener.accept();
                if (!capacity.tryAcquire()) { socket.close(); continue; }
                Handler handler = new Handler(socket);
                synchronized (lifecycle) {
                    if (!running) { handler.close(); capacity.release(); break; }
                    handler.worker = new Thread(handler, "dichvu-client-" + sequence.incrementAndGet());
                    connections.add(handler);
                    handler.worker.start();
                }
            } catch (SocketException ex) { if (running) throw ex; }
        }
    }
    private static void log(String message) { System.out.println(LocalDateTime.now() + " [" + Thread.currentThread().getName() + "] " + message); }
    private final class Handler implements Runnable, AutoCloseable {
        private final Socket socket;
        private BufferedWriter writer;
        private volatile SocketMessageWriter subscriberWriter;
        private Thread worker;
        private Handler(Socket socket) { this.socket = socket; }
        private synchronized void send(String line) throws IOException {
            writer.write(line); writer.newLine(); writer.flush();
        }
        private synchronized CompletableFuture<Void> enqueue(String line) throws IOException {
            return subscriberWriter.offer(line);
        }
        @Override public void run() {
            try (socket;
                 Reader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                socket.setSoTimeout(5000);
                String line = ServiceProtocol.readLine(reader);
                if (line == null) return;
                String[] fields = line.split("\\|", -1);
                if (fields.length < 2 || !ServiceProtocol.encode(token).equals(fields[1])) { send("ERROR|AUTH"); return; }
                if ("SUB".equals(fields[0]) && fields.length == 3) {
                    String name = ServiceProtocol.decode(fields[2]);
                    if (name.isBlank() || name.length() > 80) { send("ERROR|NAME"); return; }
                    synchronized (this) {
                        subscriberWriter = new SocketMessageWriter(socket, writer, "dichvu-send-" + Thread.currentThread().getName());
                        subscribers.add(this);
                        send("READY");
                    }
                    socket.setSoTimeout(0);
                    log("Đăng ký client " + name + "; đang kết nối=" + subscribers.size());
                    while ((line = ServiceProtocol.readLine(reader)) != null) {
                        if ("PING".equals(line)) enqueue("PONG");
                        else {
                            try { enqueue("ERROR|COMMAND").get(deliveryTimeoutMillis, TimeUnit.MILLISECONDS); }
                            catch (ExecutionException | TimeoutException ignored) { }
                            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                            break;
                        }
                    }
                } else if ("EVENT".equals(fields[0]) && fields.length == 9) {
                    long eventId = Long.parseLong(fields[2]); long stayId = Long.parseLong(fields[3]);
                    int quantity = Integer.parseInt(fields[6]);
                    if (eventId < 1 || stayId < 1 || quantity < 1 || quantity > 999) throw new IllegalArgumentException("Giá trị không hợp lệ");
                    ServiceProtocol.decode(fields[4]); ServiceProtocol.decode(fields[5]); LocalDateTime.parse(fields[7]);
                    if (fields[8].length() > 50) throw new IllegalArgumentException("Nguồn không hợp lệ");
                    String payload = "SERVICE|" + String.join("|", java.util.Arrays.copyOfRange(fields, 2, 8));
                    List<Delivery> deliveries = new ArrayList<>();
                    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(deliveryTimeoutMillis);
                    // Enqueue to ALL subscribers before waiting. One stalled receiver cannot
                    // prevent the other receivers from being offered this message.
                    for (Handler receiver : subscribers) {
                        try { deliveries.add(new Delivery(receiver, receiver.enqueue(payload))); }
                        catch (IOException ex) { receiver.close(); }
                    }
                    int recipients = 0;
                    for (Delivery delivery : deliveries) {
                        try {
                            delivery.completion().get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                            recipients++;
                        } catch (ExecutionException | TimeoutException ex) {
                            delivery.receiver().close();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    send("ACK|" + eventId + "|" + recipients);
                    log("Yêu cầu #" + eventId + " lượt ở #" + stayId + " -> " + recipients + " client");
                } else send("ERROR|FORMAT");
            } catch (IOException | IllegalArgumentException ex) {
                log("Kết nối kết thúc: " + ex.getMessage());
            } finally {
                close();
                subscribers.remove(this); connections.remove(this); capacity.release();
                log("Đóng kết nối; client nhận còn=" + subscribers.size());
            }
        }
        @Override public void close() {
            subscribers.remove(this);
            try { socket.close(); } catch (IOException ignored) { }
            if (subscriberWriter != null) subscriberWriter.close();
        }
        private void awaitStopped(long deadline) throws InterruptedException {
            long left = TimeUnit.NANOSECONDS.toMillis(Math.max(0, deadline - System.nanoTime()));
            if (worker != null && Thread.currentThread() != worker && left > 0) worker.join(left);
            left = TimeUnit.NANOSECONDS.toMillis(Math.max(0, deadline - System.nanoTime()));
            if (subscriberWriter != null) subscriberWriter.awaitStopped(left);
        }
    }
    private record Delivery(Handler receiver, CompletableFuture<Void> completion) { }
    @Override public void close() throws IOException {
        List<Handler> snapshot;
        synchronized (lifecycle) {
            running = false;
            listener.close();
            snapshot = new ArrayList<>(connections);
        }
        for (Handler handler : snapshot) handler.close();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        try { for (Handler handler : snapshot) handler.awaitStopped(deadline); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        String host = args.length > 1 ? args[1] : "127.0.0.1";
        try (DichVuSocketServer server = new DichVuSocketServer(host, port, ServiceProtocol.token())) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.close(); } catch (IOException ignored) { } }, "dichvu-shutdown"));
            server.serve();
        }
    }
}
