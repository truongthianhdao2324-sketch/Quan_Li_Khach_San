package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public String doiTrangThai(String soPhong, String trangThai) {

        System.out.println("[" + Thread.currentThread().getName()
                + "] Bắt đầu cập nhật phòng " + soPhong);

        Optional<Room> ketQua =
                roomRepository.findBySoPhongForUpdate(soPhong);

        if (ketQua.isEmpty()) {
            System.out.println("[" + Thread.currentThread().getName()
                    + "] Không tìm thấy phòng " + soPhong);
            return "Không tìm thấy phòng!";
        }

        System.out.println("[" + Thread.currentThread().getName()
                + "] Đã lấy được khóa phòng " + soPhong);

        Room phong = ketQua.get();
        phong.setTrangThai(trangThai);
        roomRepository.save(phong);

        System.out.println("[" + Thread.currentThread().getName()
                + "] Cập nhật phòng " + soPhong
                + " thành " + trangThai);

        return "Cập nhật trạng thái phòng thành công!";
    }
}