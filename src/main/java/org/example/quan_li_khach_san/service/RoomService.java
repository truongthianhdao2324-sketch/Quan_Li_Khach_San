package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public String doiTrangThai(String soPhong, String trangThaiMoi) {

        Room phong = roomRepository.findBySoPhongForUpdate(soPhong)
                .orElse(null);

        if (phong == null) {
            return "Không tìm thấy phòng!";
        }

        // Không cho hai lễ tân cùng nhận một phòng
        if ("Đã có khách".equals(trangThaiMoi)
                && !"Còn trống".equals(phong.getTrangThai())) {
            return "Phòng này vừa được nhân viên khác cập nhật. Vui lòng tải lại!";
        }

        phong.setTrangThai(trangThaiMoi);
        roomRepository.save(phong);

        return "Cập nhật trạng thái phòng thành công!";
    }
}