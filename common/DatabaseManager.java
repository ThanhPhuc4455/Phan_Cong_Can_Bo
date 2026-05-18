package common;

import model.CanBo;
import model.GiamSatEntry;
import model.PhanCongEntry;
import model.PhongThi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý cơ sở dữ l
 * iệu SQLite.
 *
 * Schema:
 *   can_bo      - Danh sách cán bộ coi thi
 *   phong_thi   - Danh sách phòng thi
 *   phan_cong   - Kết quả phân công giám thị (theo ca)
 *   giam_sat    - Kết quả phân công giám sát (theo ca)
 *   room_history- Lịch sử: phòng nào đã có cán bộ nào (để tránh trùng)
 *   pair_history- Lịch sử: cặp (GV1,GV2) đã từng cùng coi một phòng
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:exam_proctor.db";
    private Connection conn;

    public DatabaseManager() throws SQLException {
        conn = DriverManager.getConnection(DB_URL);
        conn.setAutoCommit(false);
        createTables();
    }

    // ===== Khởi tạo bảng =====

    public void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS can_bo (" +
                "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  stt       INTEGER," +
                "  ma_cb     TEXT UNIQUE," +
                "  ho_ten    TEXT," +
                "  ngay_sinh TEXT," +
                "  don_vi    TEXT" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS phong_thi (" +
                "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  stt       INTEGER," +
                "  ten_phong TEXT UNIQUE," +
                "  dia_diem  TEXT" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS phan_cong (" +
                "  id       INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  ca_thi   INTEGER," +
                "  stt      INTEGER," +
                "  ma_cb    TEXT," +
                "  ho_ten   TEXT," +
                "  vai_tro  TEXT," +
                "  phong    TEXT" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS giam_sat (" +
                "  id       INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  ca_thi   INTEGER," +
                "  stt      INTEGER," +
                "  ma_cb    TEXT," +
                "  ho_ten   TEXT," +
                "  khu_vuc  TEXT" +
                ")"
            );
            // Lịch sử: phòng thi → set các cán bộ đã coi (mọi ca)
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS room_history (" +
                "  phong    TEXT," +
                "  ma_cb    TEXT," +
                "  PRIMARY KEY (phong, ma_cb)" +
                ")"
            );
            // Lịch sử: cặp giám thị đã từng cùng phòng
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS pair_history (" +
                "  pair_key TEXT PRIMARY KEY" + // "MA1|MA2" (đã sắp xếp alphabet)
                ")"
            );
            conn.commit();
        }
    }

    // ===== Xóa dữ liệu cũ =====

    public void clearAllData() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM can_bo");
            st.executeUpdate("DELETE FROM phong_thi");
            st.executeUpdate("DELETE FROM phan_cong");
            st.executeUpdate("DELETE FROM giam_sat");
            st.executeUpdate("DELETE FROM room_history");
            st.executeUpdate("DELETE FROM pair_history");
            conn.commit();
        }
    }

    public void clearAssignmentHistory() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM phan_cong");
            st.executeUpdate("DELETE FROM giam_sat");
            st.executeUpdate("DELETE FROM room_history");
            st.executeUpdate("DELETE FROM pair_history");
            conn.commit();
        }
    }

    // ===== Lưu dữ liệu =====

    public void saveCanBoList(List<CanBo> list) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM can_bo");
        }
        String sql = "INSERT OR IGNORE INTO can_bo(stt, ma_cb, ho_ten, ngay_sinh, don_vi) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (CanBo cb : list) {
                ps.setInt   (1, cb.getStt());
                ps.setString(2, cb.getMaCB());
                ps.setString(3, cb.getHoTen());
                ps.setString(4, cb.getNgaySinh());
                ps.setString(5, cb.getDonVi());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void savePhongThiList(List<PhongThi> list) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM phong_thi");
        }
        String sql = "INSERT OR IGNORE INTO phong_thi(stt, ten_phong, dia_diem) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PhongThi p : list) {
                ps.setInt   (1, p.getStt());
                ps.setString(2, p.getTenPhong());
                ps.setString(3, p.getDiaDiem());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void savePhanCong(int caThi, List<PhanCongEntry> list) throws SQLException {
        // Xóa ca cũ (nếu có)
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM phan_cong WHERE ca_thi=?")) {
            ps.setInt(1, caThi); ps.executeUpdate();
        }
        String sql = "INSERT INTO phan_cong(ca_thi, stt, ma_cb, ho_ten, vai_tro, phong) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PhanCongEntry e : list) {
                ps.setInt   (1, caThi);
                ps.setInt   (2, e.getStt());
                ps.setString(3, e.getMaCB());
                ps.setString(4, e.getHoTen());
                ps.setString(5, e.getVaiTro());
                ps.setString(6, e.getPhongThi());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void saveGiamSat(int caThi, List<GiamSatEntry> list) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM giam_sat WHERE ca_thi=?")) {
            ps.setInt(1, caThi); ps.executeUpdate();
        }
        String sql = "INSERT INTO giam_sat(ca_thi, stt, ma_cb, ho_ten, khu_vuc) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (GiamSatEntry e : list) {
                ps.setInt   (1, caThi);
                ps.setInt   (2, e.getStt());
                ps.setString(3, e.getMaCB());
                ps.setString(4, e.getHoTen());
                ps.setString(5, e.getKhuVuc());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    /** Cập nhật lịch sử phòng: phòng → cán bộ đã coi */
    public void updateRoomHistory(String phong, String maCB1, String maCB2) throws SQLException {
        String sql = "INSERT OR IGNORE INTO room_history(phong, ma_cb) VALUES(?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phong); ps.setString(2, maCB1); ps.addBatch();
            ps.setString(1, phong); ps.setString(2, maCB2); ps.addBatch();
            ps.executeBatch();
        }
        conn.commit();
    }

    /** Cập nhật lịch sử cặp */
    public void updatePairHistory(String key) throws SQLException {
        String sql = "INSERT OR IGNORE INTO pair_history(pair_key) VALUES(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        }
        conn.commit();
    }

    // ===== Đọc lịch sử =====

    /** Kiểm tra cán bộ đã coi phòng này chưa */
    public boolean hasBeenInRoom(String phong, String maCB) throws SQLException {
        String sql = "SELECT 1 FROM room_history WHERE phong=? AND ma_cb=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phong); ps.setString(2, maCB);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    /** Kiểm tra cặp này đã từng cùng phòng chưa */
    public boolean pairUsed(String key) throws SQLException {
        String sql = "SELECT 1 FROM pair_history WHERE pair_key=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    // ===== Thống kê =====

    public int countCanBo()   throws SQLException { return count("can_bo"); }
    public int countPhongThi()throws SQLException { return count("phong_thi"); }
    public int countPhanCong()throws SQLException { return count("phan_cong"); }
    public int countGiamSat() throws SQLException { return count("giam_sat"); }

    private int count(String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int maxCaThi() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(ca_thi),0) FROM phan_cong")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }
}