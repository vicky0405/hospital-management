# Module: Appointment Booking

## Happy path

- Bệnh nhân chọn bác sĩ, ngày, khung giờ
- Hệ thống kiểm tra slot còn trống
- Đặt thành công → gửi email xác nhận

## Edge cases

- Slot đã có người → gợi ý 3 slot gần nhất
- 2 người đặt cùng lúc → chỉ 1 người thành công
- Không được đặt lịch trong quá khứ
- Bác sĩ nghỉ → không hiện slot ngày đó

## NFR

- Không oversell slot dù có concurrent request
- Email xác nhận gửi trong vòng 30 giây
