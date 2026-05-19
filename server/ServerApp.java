package server;

import common.DatabaseManager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

/**
 * Giao diện chính của SERVER.
 * Chức năng:
 *  - Khởi động / Dừng server TCP
 *  - Hiển thị nhật ký hoạt động
 *  - Hiển thị thống kê CSDL
 *  - Cho phép xóa lịch sử phân công
 */
public class ServerApp extends JFrame {

    // ===== UI Components =====
    private JTextArea    logArea;
    private JLabel       statusLabel;
    private JLabel       lblCanBo, lblPhong, lblPhanCong, lblGiamSat, lblCaThi;
    private JButton      btnStart, btnStop, btnClearHistory, btnClearAll;
    private JTextField   portField;

    // ===== Network =====
    private ServerSocket serverSocket;
    private Thread       acceptThread;
    private volatile boolean running = false;

    // ===== Database =====
    private DatabaseManager db;

    // ===== Constants =====
    private static final Color COLOR_PRIMARY   = new Color(0x1F3864);
    private static final Color COLOR_SUCCESS   = new Color(0x2E7D32);
    private static final Color COLOR_DANGER    = new Color(0xC62828);
    private static final Color COLOR_ACCENT    = new Color(0x1565C0);
    private static final Color COLOR_BG        = new Color(0xF5F5F5);
    private static final Color COLOR_CARD_BG   = new Color(0xFFFFFF);
    private static final Color COLOR_MUTED     = new Color(0x64748B);
    private static final Color COLOR_BORDER    = new Color(0xD7E0EA);
    private static final Color COLOR_LOG_BG    = new Color(0x1A1A2E);
    private static final Color COLOR_LOG_TEXT  = new Color(0x00FF88);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServerApp() {
        initDB();
        initUI();
        setVisible(true);
    }

    // ─────────────────────────────────────────────
    //  Khởi tạo CSDL
    // ─────────────────────────────────────────────
    private void initDB() {
        try {
            db = new DatabaseManager();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Lỗi khởi tạo CSDL: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // ─────────────────────────────────────────────
    //  Xây dựng giao diện
    // ─────────────────────────────────────────────
    private void initUI() {
        setTitle("🖥  SERVER – HỆ THỐNG PHÂN CÔNG CÁN BỘ COI THI");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(980, 720);
        setMinimumSize(new Dimension(920, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(COLOR_BG);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                stopServer();
                if (db != null) db.close();
                System.exit(0);
            }
        });

        add(buildTopPanel(),   BorderLayout.NORTH);
        add(buildCenterPanel(),BorderLayout.CENTER);
        add(buildStatsPanel(), BorderLayout.EAST);
        add(buildBottomPanel(),BorderLayout.SOUTH);

        refreshStats();
    }

    // ─── Top Panel: tiêu đề + điều khiển ─────────
    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setBackground(COLOR_PRIMARY);
        top.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("SERVER PHÂN CÔNG CÁN BỘ COI THI", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        top.add(title, BorderLayout.WEST);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ctrl.setOpaque(false);

        JLabel portLbl = new JLabel("Cổng:");
        portLbl.setForeground(Color.WHITE);
        portLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        portField = new JTextField("9999", 6);
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        portField.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(new Color(0x88B7FF), 1, true),
            new EmptyBorder(6, 10, 6, 10)));

        btnStart = makeButton("▶  Khởi Động", COLOR_SUCCESS);
        btnStop  = makeButton("■  Dừng Server", COLOR_DANGER);
        btnStop.setEnabled(false);

        btnStart.addActionListener(e -> startServer());
        btnStop .addActionListener(e -> stopServer());

        statusLabel = new JLabel("⏸  Chưa khởi động", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(0xFFCC00));
        statusLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
        statusLabel.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(new Color(0x88B7FF), 1, true),
            new EmptyBorder(8, 14, 8, 14)));

        ctrl.add(portLbl); ctrl.add(portField);
        ctrl.add(btnStart); ctrl.add(btnStop); ctrl.add(statusLabel);
        top.add(ctrl, BorderLayout.EAST);

        return top;
    }

    // ─── Center Panel: nhật ký log ────────────────
    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            " 📋 Nhật Ký Hoạt Động ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI Semibold", Font.BOLD, 12), COLOR_PRIMARY));
        panel.setBackground(COLOR_CARD_BG);
        panel.setBorder(new CompoundBorder(
            new EmptyBorder(0, 10, 0, 0),
            panel.getBorder()));

        JLabel intro = new JLabel("Theo dõi trạng thái lắng nghe, kết nối client và tiến trình xử lý theo thời gian thực.");
        intro.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        intro.setForeground(COLOR_MUTED);
        intro.setBorder(new EmptyBorder(10, 12, 8, 12));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(COLOR_LOG_BG);
        logArea.setForeground(COLOR_LOG_TEXT);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setMargin(new Insets(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setBorder(new EmptyBorder(0, 12, 12, 12));
        panel.add(intro, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        log("=== Hệ Thống Phân Công Cán Bộ Coi Thi ===");
        log("Nhấn 'Khởi Động' để bắt đầu lắng nghe kết nối từ Client.");

        return panel;
    }

    // ─── East Panel: thống kê DB ──────────────────
    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_CARD_BG);
        panel.setBorder(new CompoundBorder(
            new EmptyBorder(0, 0, 0, 10),
            new TitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                " 📊 Thống Kê CSDL ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI Semibold", Font.BOLD, 12), COLOR_PRIMARY)));
        panel.setPreferredSize(new Dimension(250, 0));

        lblCaThi    = makeStatLabel("Ca thi hiện tại: --");
        lblCanBo    = makeStatLabel("Cán bộ: --");
        lblPhong    = makeStatLabel("Phòng thi: --");
        lblPhanCong = makeStatLabel("Bản phân công: --");
        lblGiamSat  = makeStatLabel("Bản giám sát: --");

        panel.add(Box.createVerticalStrut(10));
        panel.add(makeSectionHint("So lieu duoc lam moi sau moi lan client xu ly xong."));
        panel.add(Box.createVerticalStrut(12));
        panel.add(lblCaThi);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblCanBo);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblPhong);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblPhanCong);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblGiamSat);
        panel.add(Box.createVerticalGlue());

        // Nút quản lý DB
        panel.add(makeSeparator());
        btnClearHistory = makeButton("🗑  Xóa Lịch Sử Ca", COLOR_DANGER);
        btnClearAll     = makeButton("❌  Xóa Tất Cả DL", new Color(0x6D4C41));
        btnClearHistory.setMaximumSize(new Dimension(200, 35));
        btnClearAll    .setMaximumSize(new Dimension(200, 35));
        btnClearHistory.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClearAll    .setAlignmentX(Component.CENTER_ALIGNMENT);

        btnClearHistory.addActionListener(e -> clearHistory());
        btnClearAll    .addActionListener(e -> clearAll());

        panel.add(btnClearHistory);
        panel.add(Box.createVerticalStrut(6));
        panel.add(btnClearAll);
        panel.add(Box.createVerticalStrut(10));

        JButton btnRefresh = makeButton("🔄  Làm Mới", COLOR_ACCENT);
        btnRefresh.setMaximumSize(new Dimension(200, 32));
        btnRefresh.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRefresh.addActionListener(e -> refreshStats());
        panel.add(btnRefresh);
        panel.add(Box.createVerticalStrut(10));

        return panel;
    }

    // ─── Bottom Panel ─────────────────────────────
    private JPanel buildBottomPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setBackground(COLOR_PRIMARY);
        JLabel lbl = new JLabel("TCP Port: 9999  |  MySQL: exam_proctor  |  Java Swing + Apache POI");
        lbl.setForeground(new Color(0xD5E3F3));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        p.add(lbl);
        return p;
    }

    // ─────────────────────────────────────────────
    //  Logic Server
    // ─────────────────────────────────────────────
    private void startServer() {
        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        running = true;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        portField.setEnabled(false);

        int finalPort = port;
        acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(finalPort);
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("🟢  Đang chạy – Cổng " + finalPort);
                    statusLabel.setForeground(new Color(0x69F0AE));
                });
                log("🚀 Server khởi động thành công tại cổng " + finalPort);
                log("   Đang chờ kết nối từ Client...");

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, db, this::log);
                    handler.start();
                    SwingUtilities.invokeLater(this::refreshStats);
                }
            } catch (IOException e) {
                if (running) log(" Lỗi server: " + e.getMessage());
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void stopServer() {
        running = false;
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); }
        catch (IOException ignored) {}
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        portField.setEnabled(true);
        statusLabel.setText("⏸  Đã dừng");
        statusLabel.setForeground(new Color(0xFFCC00));
        log(" Server đã dừng.");
    }

    // ─────────────────────────────────────────────
    //  Thống kê CSDL
    // ─────────────────────────────────────────────
    private void refreshStats() {
        try {
            lblCanBo   .setText("Cán bộ: "        + db.countCanBo()   + " người");
            lblPhong   .setText("Phòng thi: "      + db.countPhongThi()+ " phòng");
            lblPhanCong.setText("Bản phân công: "  + db.countPhanCong()+ " dòng");
            lblGiamSat .setText("Bản giám sát: "   + db.countGiamSat() + " dòng");
            lblCaThi   .setText("Ca thi tối đa: "  + db.maxCaThi());
        } catch (SQLException ignored) {}
    }

    private void clearHistory() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xóa toàn bộ lịch sử phân công (room_history, pair_history)?\n" +
            "Thao tác này không thể hoàn tác.",
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try { db.clearAssignmentHistory(); refreshStats(); log("🗑  Đã xóa lịch sử phân công."); }
            catch (SQLException e) { log("❌ Lỗi xóa lịch sử: " + e.getMessage()); }
        }
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xóa TOÀN BỘ dữ liệu trong CSDL?\nThao tác này không thể hoàn tác.",
            "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try { db.clearAllData(); refreshStats(); log("❌ Đã xóa toàn bộ dữ liệu CSDL."); }
            catch (SQLException e) { log("❌ Lỗi xóa dữ liệu: " + e.getMessage()); }
        }
    }

    // ─────────────────────────────────────────────
    //  Helpers UI
    // ─────────────────────────────────────────────
    private synchronized void log(String msg) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + msg;
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Loại bỏ setFocusPainted và setBorderPainted để FlatLaf tự render hiệu ứng (hover, border)
        return btn;
    }

    private JLabel makeStatLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(0x223047));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE5ECF3), 1, true),
            new EmptyBorder(10, 12, 10, 12)));
        return lbl;
    }

    private JLabel makeSectionHint(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COLOR_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
        return lbl;
    }

    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ─────────────────────────────────────────────
    //  Main
    // ─────────────────────────────────────────────
    public static void main(String[] args) {
        // Thiết lập Look and Feel hiện đại macOS
        try { 
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            FlatMacLightLaf.setup(); 
        }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(ServerApp::new);
    }
}
