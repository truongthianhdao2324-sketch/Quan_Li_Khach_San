package org.example.quan_li_khach_san.network;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

/** A bounded, FIFO output queue for ONE subscriber; a slow socket never owns a publisher thread. */
final class SocketMessageWriter implements AutoCloseable {
    private record Frame(String text, CompletableFuture<Void> completion) { }
    private final ArrayBlockingQueue<Frame> pending = new ArrayBlockingQueue<>(64);
    private final Socket socket;
    private final BufferedWriter out;
    private final Thread worker;
    private boolean closed;

    SocketMessageWriter(Socket socket, BufferedWriter out, String name) {
        this.socket = socket;
        this.out = out;
        worker = new Thread(this::writeMessages, name);
        worker.start();
    }

    synchronized CompletableFuture<Void> offer(String message) throws IOException {
        if (closed) throw new IOException("Kết nối nhận đã đóng.");
        CompletableFuture<Void> completion = new CompletableFuture<>();
        if (!pending.offer(new Frame(message, completion))) throw new IOException("Client đọc quá chậm, hàng đợi đã đầy.");
        return completion;
    }

    private void writeMessages() {
        Frame current = null;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                current = pending.take();
                out.write(current.text()); out.newLine(); out.flush();
                current.completion().complete(null);
                current = null;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            if (current != null) current.completion().completeExceptionally(ex);
        } finally {
            if (current != null) current.completion().completeExceptionally(new IOException("Kết nối nhận đã đóng."));
            close();
        }
    }

    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
        }
        // Closing the socket interrupts a blocked write; thread interruption alone does not.
        try { socket.close(); } catch (IOException ignored) { }
        worker.interrupt();
        Frame frame;
        while ((frame = pending.poll()) != null)
            frame.completion().completeExceptionally(new IOException("Kết nối nhận đã đóng."));
    }

    void awaitStopped(long millis) throws InterruptedException {
        if (Thread.currentThread() != worker && millis > 0) worker.join(millis);
    }
}
