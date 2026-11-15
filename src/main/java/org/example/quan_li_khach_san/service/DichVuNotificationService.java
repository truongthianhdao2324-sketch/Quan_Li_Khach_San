package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.SuDungDichVu;
import org.example.quan_li_khach_san.repository.SuDungDichVuRepository;
import org.example.quan_li_khach_san.network.ServiceProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class DichVuNotificationService {
    private static final Logger log = LoggerFactory.getLogger(DichVuNotificationService.class);
    private final SuDungDichVuRepository usages;
    private final TransactionTemplate tx;
    private final DichVuRequestCache requests;
    private final String host, token;
    private final int port;
    public DichVuNotificationService(SuDungDichVuRepository usages, PlatformTransactionManager manager, DichVuRequestCache requests,
            @Value("${qlks.socket.host:127.0.0.1}") String host,
            @Value("${qlks.socket.port:5000}") int port,
            @Value("${qlks.socket.token:demo-noi-bo}") String token) {
        this.usages = usages; this.requests = requests; this.host = host; this.port = port; this.token = token;
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Transactional(propagation = Propagation.NEVER)
    public boolean guiLai(Long id) {
        return guiLai(id, null);
    }
    @Transactional(propagation = Propagation.NEVER)
    public boolean guiLai(Long id, Long expectedStayId) {
        Notification notification = tx.execute(status -> {
            SuDungDichVu usage = usages.findById(id).orElseThrow(() -> new DichVuNotFoundException("Không tìm thấy khoản dịch vụ."));
            if (usage.getDatPhong() == null)
                throw new DichVuException("Khoản dịch vụ cũ chưa được gắn với lượt ở. Cần đối chiếu dữ liệu trước khi gửi thông báo.");
            if (expectedStayId != null && !usage.getDatPhong().getMaDatPhong().equals(expectedStayId))
                throw new DichVuException("Khoản dịch vụ không thuộc lượt lưu trú đang xem.");
            String payload = "EVENT|" + ServiceProtocol.encode(token) + "|" + usage.getId() + "|" + usage.getDatPhong().getMaDatPhong()
                    + "|" + ServiceProtocol.encode(usage.getSoPhong()) + "|" + ServiceProtocol.encode(usage.getTenDichVu())
                    + "|" + usage.getSoLuong() + "|" + usage.getNgaySuDung() + "|WEB";
            return new Notification(payload, requests.wasDelivered(id));
        });
        if (notification.alreadySent()) return true;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000); socket.setSoTimeout(3000);
            try (Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                 Reader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                out.write(notification.payload() + "\n"); out.flush();
                String reply = ServiceProtocol.readLine(in);
                if (reply == null) return false;
                String[] parts = reply.split("\\|", -1);
                // ACK with zero receivers is NOT a delivered notification. Keep retry available.
                if (parts.length != 3 || !"ACK".equals(parts[0]) || !id.toString().equals(parts[1]) || Integer.parseInt(parts[2]) < 1) return false;
                requests.markDelivered(id);
                return true;
            }
        } catch (IOException | IllegalArgumentException ex) { log.warn("TCP #{} chưa gửi: {}", id, ex.getMessage()); return false; }
    }
    public java.util.Set<Long> daGuiIds() { return requests.deliveredIds(); }

    private record Notification(String payload, boolean alreadySent) { }
}
