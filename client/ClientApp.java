package client;

import common.FileTransferUtil;
import common.Protocol;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Giao diện CLIENT – Hệ thống Phân công Cán bộ Coi thi.
 *
 * Quy trình:
 *  1. Người dùng chọn file CANBOCOITHI.xlsx và PHONGTHI.xlsx
 *  2. Nhập số ca thi và địa chỉ server
 *  3. Nhấn "Gửi & Phân công" → kết nối TCP tới Server
 *  4. Gửi: CMD_CA_NUMBER → file cán bộ → file phòng thi → CMD_PROCESS
 *  5. Nhận: DANHSACHPHANCONG.xlsx + DANHSACHGIAMSAT.xlsx
 *  6. Lưu file kết quả vào thư mục người dùng chọn
 */
public class ClientApp extends JFrame {

    // ===== Màu sắc =====
    private static final Color COLOR_PRIMARY  = new Color(0x1F3864);
    private static final Color COLOR_SUCCESS  = new Color(0x2E7D32);
    private static final Color COLOR_DANGER   = new Color(0xC62828);
    private static final Color COLOR_ACCENT   = new Color(0x1565C0);
    private static final Color COLOR_BG       = new Color(0xF5F5F5);
    private static final Color COLOR_LOG_BG   = new Color(0x1A1A2E);
    private static final Color COLOR_LOG_TEXT = new Color(0x00FF88);
    private static final Color COLOR_WARN     = new Color(0xFF6F00);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ===== UI =====
    private JTextField  txtCanBoFile;
    private JTextField  txtPhongFile;
    private JTextField  txtSaveDir;
    private JTextField  txtHost;
    private JTextField  txtPort;
    private JSpinner    spnCaThi;
    private JTextArea   logArea;
    private JButton     btnSend;
    private JButton     btnOpenPhanCong;
    private JButton     btnOpenGiamSat;
    private JProgressBar progressBar;
    private JLabel      statusLabel;

    // ===== State =====
    private File selectedCanBoFile  = null;
    private File selectedPhongFile  = null;
    private File savedPhanCongFile  = null;
    private File savedGiamSatFile   = null;

    public ClientApp() {
        initUI();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Xây dựng giao diện
    // ─────────────────────────────────────────────────────────────────
    private void initUI() {
        setTitle("📋 CLIENT – HỆ THỐNG PHÂN CÔNG CÁN BỘ COI THI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(COLOR_BG);

        add(buildTopPanel(),    BorderLayout.NORTH);
        add(buildMainPanel(),   BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    // ─── Top: tiêu đề ──────────────────────────────────────────────
    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(COLOR_PRIMARY);
        top.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("CLIENT – PHÂN CÔNG CÁN BỘ COI THI", SwingConstants.LEFT);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        statusLabel = new JLabel("⏸  Chưa kết nối", SwingConstants.RIGHT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(new Color(0xFFCC00));

        top.add(title,       BorderLayout.WEST);
        top.add(statusLabel, BorderLayout.EAST);
        return top;
    }

    // ─── Main: chia đôi trái/phải ──────────────────────────────────
    private JSplitPane buildMainPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                          buildLeftPanel(),
                                          buildRightPanel());
        split.setDividerLocation(420);
        split.setResizeWeight(0.45);
        split.setBorder(new EmptyBorder(6, 8, 6, 8));
        return split;
    }

    // ─── Trái: cấu hình + điều khiển ───────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(4, 4, 4, 8));

        panel.add(buildFileSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildConnectionSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildActionSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildResultSection());
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ─── Phần chọn file ────────────────────────────────────────────
    private JPanel buildFileSection() {
        JPanel p = titledPanel("📂  Chọn File Đầu Vào");

        // File cán bộ
        p.add(new JLabel("File CANBOCOITHI.xlsx:"));
        p.add(Box.createVerticalStrut(3));

        JPanel rowCB = new JPanel(new BorderLayout(5, 0));
        rowCB.setOpaque(false);
        txtCanBoFile = makeTextField("(chưa chọn file)");
        txtCanBoFile.setEditable(false);
        JButton btnCB = makeSmallButton("Chọn...", COLOR_ACCENT);
        btnCB.addActionListener(e -> chooseFile(true));
        rowCB.add(txtCanBoFile, BorderLayout.CENTER);
        rowCB.add(btnCB,        BorderLayout.EAST);
        p.add(rowCB);
        p.add(Box.createVerticalStrut(10));

        // File phòng thi
        p.add(new JLabel("File PHONGTHI.xlsx:"));
        p.add(Box.createVerticalStrut(3));

        JPanel rowPT = new JPanel(new BorderLayout(5, 0));
        rowPT.setOpaque(false);
        txtPhongFile = makeTextField("(chưa chọn file)");
        txtPhongFile.setEditable(false);
        JButton btnPT = makeSmallButton("Chọn...", COLOR_ACCENT);
        btnPT.addActionListener(e -> chooseFile(false));
        rowPT.add(txtPhongFile, BorderLayout.CENTER);
        rowPT.add(btnPT,        BorderLayout.EAST);
        p.add(rowPT);
        p.add(Box.createVerticalStrut(10));

        // Thư mục lưu
        p.add(new JLabel("Thư mục lưu kết quả:"));
        p.add(Box.createVerticalStrut(3));

        JPanel rowSave = new JPanel(new BorderLayout(5, 0));
        rowSave.setOpaque(false);
        txtSaveDir = makeTextField(System.getProperty("user.home"));
        JButton btnSave = makeSmallButton("Chọn...", COLOR_ACCENT);
        btnSave.addActionListener(e -> chooseSaveDir());
        rowSave.add(txtSaveDir, BorderLayout.CENTER);
        rowSave.add(btnSave,    BorderLayout.EAST);
        p.add(rowSave);

        return p;
    }

    // ─── Phần kết nối ──────────────────────────────────────────────
    private JPanel buildConnectionSection() {
        JPanel p = titledPanel("🌐  Kết Nối Server");

        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0));
        row1.setOpaque(false);

        JPanel colHost = new JPanel(new BorderLayout(0, 3));
        colHost.setOpaque(false);
        colHost.add(new JLabel("Địa chỉ Server:"), BorderLayout.NORTH);
        txtHost = makeTextField("127.0.0.1");
        colHost.add(txtHost, BorderLayout.CENTER);

        JPanel colPort = new JPanel(new BorderLayout(0, 3));
        colPort.setOpaque(false);
        colPort.add(new JLabel("Cổng:"), BorderLayout.NORTH);
        txtPort = makeTextField(String.valueOf(Protocol.PORT));
        colPort.add(txtPort, BorderLayout.CENTER);

        row1.add(colHost);
        row1.add(colPort);
        p.add(row1);
        p.add(Box.createVerticalStrut(10));

        JPanel row2 = new JPanel(new BorderLayout(0, 3));
        row2.setOpaque(false);
        row2.add(new JLabel("Số ca thi:"), BorderLayout.NORTH);
        SpinnerNumberModel model = new SpinnerNumberModel(1, 1, 99, 1);
        spnCaThi = new JSpinner(model);
        spnCaThi.setFont(new Font("Arial", Font.PLAIN, 13));
        row2.add(spnCaThi, BorderLayout.WEST);
        p.add(row2);

        return p;
    }

    // ─── Phần nút hành động ────────────────────────────────────────
    private JPanel buildActionSection() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        btnSend = makeButton("📤  Gửi & Phân Công", COLOR_SUCCESS);
        btnSend.setPreferredSize(new Dimension(0, 46));
        btnSend.setFont(new Font("Arial", Font.BOLD, 15));
        btnSend.addActionListener(e -> sendRequest());

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng");
        progressBar.setFont(new Font("Arial", Font.PLAIN, 11));
        progressBar.setPreferredSize(new Dimension(0, 22));

        p.add(btnSend,      BorderLayout.CENTER);
        p.add(progressBar,  BorderLayout.SOUTH);
        return p;
    }

    // ─── Phần xem kết quả ──────────────────────────────────────────
    private JPanel buildResultSection() {
        JPanel p = titledPanel("📊  Kết Quả Nhận Về");

        JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
        row.setOpaque(false);

        btnOpenPhanCong = makeSmallButton("📋 Mở Phân Công", COLOR_ACCENT);
        btnOpenPhanCong.setEnabled(false);
        btnOpenPhanCong.addActionListener(e -> openFile(savedPhanCongFile));

        btnOpenGiamSat = makeSmallButton("🔍 Mở Giám Sát", new Color(0x6A1B9A));
        btnOpenGiamSat.setEnabled(false);
        btnOpenGiamSat.addActionListener(e -> openFile(savedGiamSatFile));

        row.add(btnOpenPhanCong);
        row.add(btnOpenGiamSat);
        p.add(row);

        return p;
    }

    // ─── Phải: nhật ký ─────────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);
        panel.setBorder(new TitledBorder(
            BorderFactory.createLineBorder(COLOR_PRIMARY, 1),
            " 📋 Nhật Ký Hoạt Động ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), COLOR_PRIMARY));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(COLOR_LOG_BG);
        logArea.setForeground(COLOR_LOG_TEXT);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scroll, BorderLayout.CENTER);

        // Nút xóa log
        JButton btnClearLog = makeSmallButton("🗑 Xóa Log", COLOR_DANGER);
        btnClearLog.addActionListener(e -> logArea.setText(""));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        btnRow.setBackground(COLOR_BG);
        btnRow.add(btnClearLog);
        panel.add(btnRow, BorderLayout.SOUTH);

        log("=== Client Phân Công Cán Bộ Coi Thi ===");
        log("1. Chọn file CANBOCOITHI.xlsx và PHONGTHI.xlsx");
        log("2. Nhập địa chỉ server và số ca thi");
        log("3. Nhấn 'Gửi & Phân Công' để bắt đầu");

        return panel;
    }

    // ─── Bottom ────────────────────────────────────────────────────
    private JPanel buildBottomPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setBackground(COLOR_PRIMARY);
        JLabel lbl = new JLabel(
            "TCP Client  |  Giao thức: CMD_CA_NUMBER → FILE → CMD_PROCESS → nhận kết quả  |  Apache POI");
        lbl.setForeground(new Color(0xCCCCCC));
        lbl.setFont(new Font("Arial", Font.PLAIN, 11));
        p.add(lbl);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Xử lý sự kiện
    // ─────────────────────────────────────────────────────────────────

    /** Chọn file Excel đầu vào */
    private void chooseFile(boolean isCanBo) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(isCanBo ? "Chọn file CANBOCOITHI.xlsx" : "Chọn file PHONGTHI.xlsx");
        fc.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
        fc.setCurrentDirectory(new File(System.getProperty("user.home")));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (isCanBo) {
                selectedCanBoFile = f;
                txtCanBoFile.setText(f.getAbsolutePath());
                log("📂 Đã chọn file cán bộ: " + f.getName());
            } else {
                selectedPhongFile = f;
                txtPhongFile.setText(f.getAbsolutePath());
                log("📂 Đã chọn file phòng thi: " + f.getName());
            }
        }
    }

    /** Chọn thư mục lưu kết quả */
    private void chooseSaveDir() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Chọn thư mục lưu kết quả");
        fc.setCurrentDirectory(new File(txtSaveDir.getText()));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtSaveDir.setText(fc.getSelectedFile().getAbsolutePath());
            log("📁 Thư mục lưu: " + fc.getSelectedFile().getAbsolutePath());
        }
    }

    /** Mở file bằng ứng dụng mặc định hệ thống */
    private void openFile(File f) {
        if (f == null || !f.exists()) {
            JOptionPane.showMessageDialog(this,
                "File không tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(f);
        } catch (IOException e) {
            log("⚠️  Không thể mở file: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Không thể mở file:\n" + f.getAbsolutePath(),
                "Lỗi", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  GỬI YÊU CẦU LÊN SERVER (chạy trên worker thread)
    // ─────────────────────────────────────────────────────────────────
    private void sendRequest() {
        // ── Validate đầu vào ──────────────────────────────────────────
        if (selectedCanBoFile == null || !selectedCanBoFile.exists()) {
            showError("Vui lòng chọn file CANBOCOITHI.xlsx!");
            return;
        }
        if (selectedPhongFile == null || !selectedPhongFile.exists()) {
            showError("Vui lòng chọn file PHONGTHI.xlsx!");
            return;
        }
        String host = txtHost.getText().trim();
        if (host.isEmpty()) { showError("Vui lòng nhập địa chỉ server!"); return; }

        int port;
        try { port = Integer.parseInt(txtPort.getText().trim()); }
        catch (NumberFormatException e) { showError("Cổng không hợp lệ!"); return; }

        int caThi = (int) spnCaThi.getValue();
        File saveDir = new File(txtSaveDir.getText().trim());
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            showError("Không thể tạo thư mục lưu: " + saveDir.getAbsolutePath());
            return;
        }

        // ── Khóa UI, bắt đầu xử lý ───────────────────────────────────
        setUIEnabled(false);
        setStatus("🔄  Đang kết nối...", COLOR_WARN);
        progressBar.setIndeterminate(true);
        progressBar.setString("Đang xử lý...");

        // ── Chạy trên background thread để không đơ GUI ───────────────
        final String finalHost  = host;
        final int    finalPort  = port;
        new Thread(() -> {
            try {
                doTransfer(finalHost, finalPort, caThi, saveDir);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    setUIEnabled(true);
                    progressBar.setIndeterminate(false);
                });
            }
        }, "ClientWorker").start();
    }

    /**
     * Thực hiện toàn bộ giao tiếp Client ↔ Server:
     *  CMD_CA_NUMBER → FILE_CANBO → FILE_PHONG → CMD_PROCESS
     *  ← CMD_SEND_FILE (phanCong) ← CMD_SEND_FILE (giamSat) ← CMD_DONE
     */
    private void doTransfer(String host, int port, int caThi, File saveDir) {
        log("🔌 Đang kết nối tới " + host + ":" + port + " ...");

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream  in  = new DataInputStream(socket.getInputStream())) {

            setStatus("🟢  Đã kết nối", COLOR_SUCCESS);
            log("✅ Kết nối thành công!");

            // ── Bước 1: Gửi số ca thi ─────────────────────────────────
            log("📤 Gửi số ca thi: " + caThi);
            out.writeInt(Protocol.CMD_CA_NUMBER);
            out.writeInt(caThi);
            out.flush();

            // ── Bước 2: Gửi CANBOCOITHI.xlsx ─────────────────────────
            log("📤 Gửi file: " + selectedCanBoFile.getName()
                + " (" + selectedCanBoFile.length() + " bytes)");
            out.writeInt(Protocol.CMD_SEND_FILE);
            FileTransferUtil.sendFile(out, selectedCanBoFile);

            // ── Bước 3: Gửi PHONGTHI.xlsx ────────────────────────────
            log("📤 Gửi file: " + selectedPhongFile.getName()
                + " (" + selectedPhongFile.length() + " bytes)");
            out.writeInt(Protocol.CMD_SEND_FILE);
            FileTransferUtil.sendFile(out, selectedPhongFile);

            // ── Bước 4: Yêu cầu xử lý ────────────────────────────────
            log("⚙️  Gửi lệnh xử lý phân công...");
            out.writeInt(Protocol.CMD_PROCESS);
            out.flush();

            setStatus("⏳  Server đang xử lý...", COLOR_WARN);
            setProgress("Server đang chạy thuật toán...");

            // ── Bước 5: Nhận kết quả từ Server ───────────────────────
            int cmd = in.readInt();

            // Kiểm tra lỗi ngay từ đầu
            if (cmd == Protocol.CMD_ERROR) {
                String errMsg = readErrorMsg(in);
                log("❌ Server báo lỗi: " + errMsg);
                setStatus("❌  Lỗi từ server", COLOR_DANGER);
                setProgress("Thất bại");
                showError("Server trả về lỗi:\n" + errMsg);
                return;
            }

            // Nhận DANHSACHPHANCONG.xlsx
            if (cmd != Protocol.CMD_SEND_FILE) {
                log("❌ Giao thức không hợp lệ: nhận được cmd=" + cmd);
                setStatus("❌  Lỗi giao thức", COLOR_DANGER);
                showError("Lỗi giao thức: lệnh không mong đợi (" + cmd + ")");
                return;
            }

            File phanCongFile = FileTransferUtil.receiveFile(in, saveDir);
            log("📥 Đã nhận: " + phanCongFile.getName()
                + " (" + phanCongFile.length() + " bytes)");
            savedPhanCongFile = phanCongFile;

            // Nhận DANHSACHGIAMSAT.xlsx
            cmd = in.readInt();
            if (cmd != Protocol.CMD_SEND_FILE) {
                log("⚠️  Thiếu file giám sát (cmd=" + cmd + ")");
            } else {
                File giamSatFile = FileTransferUtil.receiveFile(in, saveDir);
                log("📥 Đã nhận: " + giamSatFile.getName()
                    + " (" + giamSatFile.length() + " bytes)");
                savedGiamSatFile = giamSatFile;
                cmd = in.readInt(); // Đọc CMD_DONE
            }

            // Kiểm tra CMD_DONE
            if (cmd == Protocol.CMD_DONE) {
                log("✅ Hoàn tất! Kết quả được lưu tại: " + saveDir.getAbsolutePath());
                setStatus("✅  Hoàn thành!", COLOR_SUCCESS);
                setProgress("Hoàn tất – file đã lưu vào: " + saveDir.getName());

                // Kích hoạt nút mở file
                SwingUtilities.invokeLater(() -> {
                    btnOpenPhanCong.setEnabled(savedPhanCongFile != null);
                    btnOpenGiamSat .setEnabled(savedGiamSatFile  != null);
                });

                // Thông báo thành công
                SwingUtilities.invokeLater(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Phân công ca ").append(caThi).append(" hoàn tất!\n\n");
                    if (savedPhanCongFile != null)
                        sb.append("📋 ").append(savedPhanCongFile.getName()).append("\n");
                    if (savedGiamSatFile != null)
                        sb.append("🔍 ").append(savedGiamSatFile.getName()).append("\n");
                    sb.append("\nĐã lưu tại:\n").append(saveDir.getAbsolutePath());
                    JOptionPane.showMessageDialog(ClientApp.this,
                        sb.toString(), "✅ Thành Công", JOptionPane.INFORMATION_MESSAGE);
                });
            } else if (cmd == Protocol.CMD_ERROR) {
                String errMsg = readErrorMsg(in);
                log("❌ Lỗi cuối: " + errMsg);
                setStatus("❌  Lỗi", COLOR_DANGER);
            }

        } catch (java.net.ConnectException e) {
            log("❌ Không thể kết nối: " + e.getMessage());
            log("   → Hãy kiểm tra server đang chạy tại " + host + ":" + port);
            setStatus("❌  Không kết nối được", COLOR_DANGER);
            setProgress("Thất bại – kiểm tra server");
            SwingUtilities.invokeLater(() -> showError(
                "Không thể kết nối tới server!\n" +
                "Host: " + host + "  Cổng: " + port + "\n\n" +
                "Hãy đảm bảo server đang chạy."));
        } catch (java.net.SocketTimeoutException e) {
            log("❌ Hết thời gian chờ: " + e.getMessage());
            setStatus("❌  Timeout", COLOR_DANGER);
            setProgress("Thất bại – timeout");
        } catch (IOException e) {
            log("❌ Lỗi I/O: " + e.getMessage());
            setStatus("❌  Lỗi kết nối", COLOR_DANGER);
            setProgress("Thất bại");
        }
    }

    /** Đọc thông điệp lỗi từ stream (sau khi đã đọc CMD_ERROR) */
    private String readErrorMsg(DataInputStream in) {
        try {
            int len = in.readInt();
            byte[] b = new byte[len];
            in.readFully(b);
            return new String(b);
        } catch (IOException e) {
            return "(không đọc được lỗi)";
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers UI
    // ─────────────────────────────────────────────────────────────────

    private void setUIEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            btnSend.setEnabled(enabled);
            spnCaThi.setEnabled(enabled);
            txtHost.setEnabled(enabled);
            txtPort.setEnabled(enabled);
        });
    }

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    private void setProgress(String text) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setString(text);
            progressBar.setIndeterminate(false);
        });
    }

    private synchronized void log(String msg) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + msg;
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE));
    }

    // ── Widget builders ───────────────────────────────────────────────

    private JPanel titledPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(COLOR_BG);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new CompoundBorder(
            new TitledBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 1),
                " " + title + " ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12), COLOR_ACCENT),
            new EmptyBorder(6, 8, 8, 8)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setFont(new Font("Arial", Font.PLAIN, 12));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return tf;
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }

    private JButton makeSmallButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Main
    // ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ClientApp::new);
    }
}
