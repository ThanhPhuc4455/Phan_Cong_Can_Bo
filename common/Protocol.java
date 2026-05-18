package common;

/**
 * Giao thức truyền thông Client/Server.
 *
 * Định dạng gói tin:
 *   - Lệnh (CMD):  4 bytes (int) mã lệnh
 *   - Dữ liệu:     Phụ thuộc từng lệnh
 *
 * Quy trình tổng thể:
 *   1. Client kết nối TCP tới Server
 *   2. Client gửi CMD_CA_NUMBER + số ca (int)
 *   3. Client gửi CMD_SEND_FILE + tên file + nội dung (CANBOCOITHI.xlsx)
 *   4. Client gửi CMD_SEND_FILE + tên file + nội dung (PHONGTHI.xlsx)
 *   5. Client gửi CMD_PROCESS → Server xử lý phân công
 *   6. Server gửi CMD_SEND_FILE + DANHSACHPHANCONG.xlsx
 *   7. Server gửi CMD_SEND_FILE + DANHSACHGIAMSAT.xlsx
 *   8. Server gửi CMD_DONE → Client lưu kết quả
 */
public class Protocol {

    // ===== Cổng kết nối =====
    public static final int    PORT        = 9999;
    public static final int    BUFFER_SIZE = 65536; // 64KB buffer

    // ===== Tên file chuẩn =====
    public static final String FILE_CANBO    = "CANBOCOITHI.xlsx";
    public static final String FILE_PHONGTHI = "PHONGTHI.xlsx";
    public static final String FILE_PHANCONG = "DANHSACHPHANCONG.xlsx";
    public static final String FILE_GIAMSAT  = "DANHSACHGIAMSAT.xlsx";

    // ===== Mã lệnh (4 bytes int) =====
    public static final int CMD_CA_NUMBER = 1001; // Gửi số ca thi (theo sau là int)
    public static final int CMD_SEND_FILE = 1002; // Gửi file (theo sau: nameLen+name+dataLen+data)
    public static final int CMD_PROCESS   = 1003; // Yêu cầu server xử lý phân công
    public static final int CMD_DONE      = 1004; // Kết thúc, mọi thứ OK
    public static final int CMD_ERROR     = 1099; // Lỗi (theo sau: msgLen + msgBytes)

    private Protocol() {} // Không khởi tạo
}