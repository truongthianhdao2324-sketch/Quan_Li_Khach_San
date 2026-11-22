package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.network.DichVuSocketServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ThongBaoDichVuService {
    private final DichVuService dichVuService;
    private final String diaChi;
    private final int cong;
    private final LinkedHashSet<Long> daGui = new LinkedHashSet<>();

    public ThongBaoDichVuService(DichVuService dichVuService,
            @Value("${qlks.socket.host:127.0.0.1}") String diaChi,
            @Value("${qlks.socket.port:5000}") int cong) {
        this.dichVuService = dichVuService;
        this.diaChi = diaChi;
        this.cong = cong;
    }

    public boolean guiLai(Long id, Long maDatPhong) {
        String noiDung = dichVuService.noiDungThongBao(id, maDatPhong);
        synchronized (daGui) { if (daGui.contains(id)) return true; }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(diaChi, cong), 2000);
            socket.setSoTimeout(3000);
            Writer dauRa = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            Reader dauVao = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            dauRa.write(noiDung + "\n"); dauRa.flush();
            String phanHoi = DichVuSocketServer.docDong(dauVao);
            if (phanHoi == null) return false;
            String[] phan = phanHoi.split("\\|", -1);
            if (phan.length != 3 || !phan[0].equals("DA_GUI") || !phan[1].equals(id.toString()) || Integer.parseInt(phan[2]) < 1) return false;
            synchronized (daGui) {
                daGui.add(id);
                if (daGui.size() > 1000) daGui.remove(daGui.iterator().next());
            }
            return true;
        } catch (IOException | IllegalArgumentException loi) {
            System.err.println("Khoản #" + id + " đã lưu, chưa gửi được thông báo: " + loi.getMessage());
            return false;
        }
    }
    public Set<Long> daGuiIds() { synchronized (daGui) { return Set.copyOf(daGui); } }
}
