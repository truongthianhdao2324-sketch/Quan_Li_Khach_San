package org.example.quan_li_khach_san.network;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class SocketMessageWriterTests {
    @Test void stalledWriterIsBoundedAndCloseReleasesEveryWaitingDelivery() throws Exception {
        CountDownLatch blocked = new CountDownLatch(1), disconnected = new CountDownLatch(1);
        Socket socket = new Socket() { @Override public void close() { disconnected.countDown(); } };
        BufferedWriter sink = new BufferedWriter(new StringWriter()) {
            @Override public void write(String text, int offset, int length) throws IOException {
                blocked.countDown();
                try { disconnected.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                throw new IOException("Simulated stalled receiver disconnected");
            }
        };
        SocketMessageWriter writer = new SocketMessageWriter(socket, sink, "test-stalled-writer");
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try {
            futures.add(writer.offer("first")); assertTrue(blocked.await(2, TimeUnit.SECONDS));
            for (int i = 0; i < 64; i++) futures.add(writer.offer("queued-" + i));
            assertThrows(IOException.class, () -> writer.offer("queue-overflow"));
        } finally { writer.close(); writer.awaitStopped(2000); }
        for (var future : futures) assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));
        assertThrows(IOException.class, () -> writer.offer("after-close"));
    }
}
