package common;

import common.model.CanBo;
import common.model.GiamSatEntry;
import common.model.PhanCongEntry;
import common.model.PhongThi;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Quản lý cơ sở dữ liệu MySQL.
 *
 * ⚠️ Đổi thông tin kết nối bên dưới cho khớp với MySQL của bạn.
 */
public class DatabaseManager {

    // ===================================================
    //  ⚙️  THAY ĐỔI THÔNG TIN KẾT NỐI MySQL Ở ĐÂY
    // ===================================================
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "exam_proctor";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "123456";  // ← MẬT KHẨU CỦA BẠN
    // ===================================================

    private static final String DB_URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=false&allowPublicKeyRetrieval=true"
        + "&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    private Connection conn;

    public DatabaseManager() throws SQLException {
        // Tạo database nếu chưa tồn tại
        String urlNoDB = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";
        try (Connection tmp = DriverManager.getConnection(urlNoDB, DB_USER, DB_PASS);
             Statement st = tmp.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DB_NAME
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        conn.setAutoCommit(false);
        createTables();
    }

    public void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS can_bo (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  stt INT, ma_cb VARCHAR(50) UNIQUE," +
                "  ho_ten VARCHAR(200), ngay_sinh VARCHAR(20), don_vi VARCHAR(200)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS phong_thi (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  stt INT, ten_phong VARCHAR(50) UNIQUE, dia_diem VARCHAR(200)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS phan_cong (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  ca_thi INT, stt INT, ma_cb VARCHAR(50)," +
                "  ho_ten VARCHAR(200), vai_tro VARCHAR(50), phong VARCHAR(50)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS giam_sat (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  ca_thi INT, stt INT, ma_cb VARCHAR(50)," +
                "  ho_ten VARCHAR(200), khu_vuc VARCHAR(200)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS room_history (" +
                "  phong VARCHAR(50), ma_cb VARCHAR(50)," +
                "  PRIMARY KEY (phong, ma_cb)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS pair_history (" +
                "  pair_key VARCHAR(120) PRIMARY KEY" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            conn.commit();
        }
    }

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

    public void saveCanBoList(List<CanBo> list) throws SQLException {
        try (Statement st = conn.createStatement()) { st.executeUpdate("DELETE FROM can_bo"); }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO can_bo(stt,ma_cb,ho_ten,ngay_sinh,don_vi) VALUES(?,?,?,?,?)")) {
            for (CanBo cb : list) {
                ps.setInt(1, cb.getStt()); ps.setString(2, cb.getMaCB());
                ps.setString(3, cb.getHoTen()); ps.setString(4, cb.getNgaySinh());
                ps.setString(5, cb.getDonVi()); ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void savePhongThiList(List<PhongThi> list) throws SQLException {
        try (Statement st = conn.createStatement()) { st.executeUpdate("DELETE FROM phong_thi"); }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO phong_thi(stt,ten_phong,dia_diem) VALUES(?,?,?)")) {
            for (PhongThi p : list) {
                ps.setInt(1, p.getStt()); ps.setString(2, p.getTenPhong());
                ps.setString(3, p.getDiaDiem()); ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void savePhanCong(int caThi, List<PhanCongEntry> list) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM phan_cong WHERE ca_thi=?")) {
            ps.setInt(1, caThi); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO phan_cong(ca_thi,stt,ma_cb,ho_ten,vai_tro,phong) VALUES(?,?,?,?,?,?)")) {
            for (PhanCongEntry e : list) {
                ps.setInt(1, caThi); ps.setInt(2, e.getStt());
                ps.setString(3, e.getMaCB()); ps.setString(4, e.getHoTen());
                ps.setString(5, e.getVaiTro()); ps.setString(6, e.getPhongThi());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void saveGiamSat(int caThi, List<GiamSatEntry> list) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM giam_sat WHERE ca_thi=?")) {
            ps.setInt(1, caThi); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO giam_sat(ca_thi,stt,ma_cb,ho_ten,khu_vuc) VALUES(?,?,?,?,?)")) {
            for (GiamSatEntry e : list) {
                ps.setInt(1, caThi); ps.setInt(2, e.getStt());
                ps.setString(3, e.getMaCB()); ps.setString(4, e.getHoTen());
                ps.setString(5, e.getKhuVuc()); ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void updateRoomHistory(String phong, String maCB1, String maCB2) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO room_history(phong,ma_cb) VALUES(?,?)")) {
            ps.setString(1, phong); ps.setString(2, maCB1); ps.addBatch();
            ps.setString(1, phong); ps.setString(2, maCB2); ps.addBatch();
            ps.executeBatch();
        }
        conn.commit();
    }

    public void updatePairHistory(String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO pair_history(pair_key) VALUES(?)")) {
            ps.setString(1, key); ps.executeUpdate();
        }
        conn.commit();
    }

    public boolean hasBeenInRoom(String phong, String maCB) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM room_history WHERE phong=? AND ma_cb=? LIMIT 1")) {
            ps.setString(1, phong); ps.setString(2, maCB);
            return ps.executeQuery().next();
        }
    }

    public boolean pairUsed(String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM pair_history WHERE pair_key=? LIMIT 1")) {
            ps.setString(1, key);
            return ps.executeQuery().next();
        }
    }

    public Map<String, Set<String>> getAllRoomHistory() throws SQLException {
        Map<String, Set<String>> map = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT phong, ma_cb FROM room_history")) {
            while (rs.next()) {
                String phong = rs.getString("phong");
                String maCB = rs.getString("ma_cb");
                map.computeIfAbsent(phong, k -> new HashSet<>()).add(maCB);
            }
        }
        return map;
    }

    public Set<String> getAllPairHistory() throws SQLException {
        Set<String> set = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT pair_key FROM pair_history")) {
            while (rs.next()) {
                set.add(rs.getString("pair_key"));
            }
        }
        return set;
    }

    public int countCanBo()    throws SQLException { return count("can_bo"); }
    public int countPhongThi() throws SQLException { return count("phong_thi"); }
    public int countPhanCong() throws SQLException { return count("phan_cong"); }
    public int countGiamSat()  throws SQLException { return count("giam_sat"); }

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