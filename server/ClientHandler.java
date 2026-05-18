package server;

import common.*;
import common.model.*;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Luồng xử lý từng kết nối Client trên Server.
 *
 * Giao thức xử lý:
 *  1. Nhận CMD_CA_NUMBER  → số ca thi
 *  2. Nhận CMD_SEND_FILE  → CANBOCOITHI.xlsx
 *  3. Nhận CMD_SEND_FILE  → PHONGTHI.xlsx
 *  4. Nhận CMD_PROCESS    → thực hiện phân công
 *  5. Gửi  CMD_SEND_FILE  → DANHSACHPHANCONG.xlsx
 *  6. Gửi  CMD_SEND_FILE  → DANHSACHGIAMSAT.xlsx
 *  7. Gửi  CMD_DONE
 */
public class ClientHandler extends Thread {

    private final Socket          socket;
    private final DatabaseManager db;
    private final Consumer<String> logger;   // Hàm callback ghi log vào GUI server

    public ClientHandler(Socket socket, DatabaseManager db, Consumer<String> logger) {
        this.socket = socket;
        this.db     = db;
        this.logger = logger;
        setDaemon(true);
    }

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress();
        log("✅ Client kết nối: " + clientAddr);

        try (DataInputStream  in  = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // ── Bước 1: Nhận số ca thi ────────────────────────────────────
            int cmd = in.readInt();
            if (cmd != Protocol.CMD_CA_NUMBER) { sendError(out, "Lệnh không hợp lệ."); return; }
            int caThi = in.readInt();
            log("📋 Ca thi nhận được: " + caThi);

            // ── Bước 2: Nhận CANBOCOITHI.xlsx ─────────────────────────────
            cmd = in.readInt();
            if (cmd != Protocol.CMD_SEND_FILE) { sendError(out, "Chờ file cán bộ."); return; }
            byte[][] canBoFile = FileTransferUtil.receiveBytes(in);
            String canBoFileName = new String(canBoFile[0]);
            log("📥 Đã nhận: " + canBoFileName + " (" + canBoFile[1].length + " bytes)");

            // ── Bước 3: Nhận PHONGTHI.xlsx ────────────────────────────────
            cmd = in.readInt();
            if (cmd != Protocol.CMD_SEND_FILE) { sendError(out, "Chờ file phòng thi."); return; }
            byte[][] phongFile = FileTransferUtil.receiveBytes(in);
            String phongFileName = new String(phongFile[0]);
            log("📥 Đã nhận: " + phongFileName + " (" + phongFile[1].length + " bytes)");

            // ── Bước 4: Nhận CMD_PROCESS ──────────────────────────────────
            cmd = in.readInt();
            if (cmd != Protocol.CMD_PROCESS) { sendError(out, "Chờ lệnh xử lý."); return; }
            log("⚙️  Bắt đầu xử lý phân công ca " + caThi + "...");

            // ── Đọc Excel ─────────────────────────────────────────────────
            List<CanBo>    canBoList  = ExcelReader.readCanBo(canBoFile[1]);
            List<PhongThi> phongList  = ExcelReader.readPhongThi(phongFile[1]);

            log(String.format("   → Cán bộ: %d người | Phòng thi: %d phòng",
                              canBoList.size(), phongList.size()));

            if (canBoList.size() < phongList.size() * 2) {
                log("   ⚠️  Cảnh báo: Số cán bộ không đủ cho 2 giám thị/phòng.");
            }

            // ── Lưu DB ────────────────────────────────────────────────────
            db.saveCanBoList(canBoList);
            db.savePhongThiList(phongList);
            log("   → Đã lưu dữ liệu vào CSDL.");

            // ── Thuật toán phân công ──────────────────────────────────────
            AssignmentAlgorithm algo   = new AssignmentAlgorithm(db);
            
            java.util.Map<Integer, List<PhanCongEntry>> phanCongMap = new java.util.LinkedHashMap<>();
            java.util.Map<Integer, List<GiamSatEntry>> giamSatMap = new java.util.LinkedHashMap<>();

            for (int i = 1; i <= caThi; i++) {
                log("⚙️  Đang xử lý phân công ca " + i + "...");
                AssignmentAlgorithm.AssignmentResult result =
                        algo.assign(canBoList, phongList, i);

                List<PhanCongEntry> phanCong = result.phanCong;
                List<GiamSatEntry>  giamSat  = result.giamSat;

                log(String.format("   → Ca %d - Phân công: %d giám thị | Giám sát: %d người",
                                  i, phanCong.size() / 2, giamSat.size()));

                // ── Lưu kết quả vào DB ────────────────────────────────────────
                db.savePhanCong(i, phanCong);
                db.saveGiamSat(i, giamSat);
                
                phanCongMap.put(i, phanCong);
                giamSatMap.put(i, giamSat);
            }

            // ── Xuất Excel (Mỗi ca 1 sheet) ─────────────────────────────
            byte[] phanCongExcel = ExcelWriter.writePhanCong(phanCongMap);
            byte[] giamSatExcel  = ExcelWriter.writeGiamSat(giamSatMap);
            log("   → Đã xuất file Excel kết quả chứa " + caThi + " sheet.");

            // ── Bước 5+6: Gửi kết quả về Client ──────────────────────────
            out.writeInt(Protocol.CMD_SEND_FILE);
            FileTransferUtil.sendBytes(out, Protocol.FILE_PHANCONG, phanCongExcel);
            log("📤 Đã gửi: " + Protocol.FILE_PHANCONG);

            out.writeInt(Protocol.CMD_SEND_FILE);
            FileTransferUtil.sendBytes(out, Protocol.FILE_GIAMSAT, giamSatExcel);
            log("📤 Đã gửi: " + Protocol.FILE_GIAMSAT);

            // ── Bước 7: Done ──────────────────────────────────────────────
            out.writeInt(Protocol.CMD_DONE);
            out.flush();
            log("✅ Hoàn tất phân công ca " + caThi + " – Client " + clientAddr);

        } catch (IOException | SQLException e) {
            log("❌ Lỗi xử lý client " + clientAddr + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void sendError(DataOutputStream out, String msg) {
        try {
            out.writeInt(Protocol.CMD_ERROR);
            byte[] b = msg.getBytes();
            out.writeInt(b.length);
            out.write(b);
            out.flush();
            log("⚠️  Gửi lỗi về client: " + msg);
        } catch (IOException ignored) {}
    }

    private void log(String msg) {
        if (logger != null) logger.accept(msg);
        System.out.println("[SERVER] " + msg);
    }
}