package common;

import model.GiamSatEntry;
import model.PhanCongEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.util.List;

/**
 * Xuất kết quả phân công ra file Excel (Apache POI).
 *
 * DANHSACHPHANCONG.xlsx:
 *   STT | Mã GV | Họ và tên | Vai trò | Phòng thi
 *
 * DANHSACHGIAMSAT.xlsx:
 *   STT | Mã GV | Họ và tên | Khu vực giám sát
 */
public class ExcelWriter {

    // Màu header: xanh navy
    private static final String HEADER_COLOR_PHANCONG = "1F3864";
    private static final String HEADER_COLOR_GIAMSAT  = "1F3864";
    // Màu xen kẽ dòng
    private static final String ROW_COLOR_GT1  = "D9E1F2"; // xanh nhạt - giám thị 1
    private static final String ROW_COLOR_GT2  = "FFFFFF"; // trắng     - giám thị 2
    private static final String ROW_COLOR_EVEN = "EAF4EA"; // xanh lá nhạt - giám sát

    /**
     * Xuất DANHSACHPHANCONG.xlsx ra mảng byte
     */
    public static byte[] writePhanCong(List<PhanCongEntry> entries, int caThi) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Phân Công Ca " + caThi);

            // Độ rộng cột
            sheet.setColumnWidth(0, 1500);   // STT
            sheet.setColumnWidth(1, 4000);   // Mã GV
            sheet.setColumnWidth(2, 8000);   // Họ tên
            sheet.setColumnWidth(3, 4000);   // Vai trò
            sheet.setColumnWidth(4, 4000);   // Phòng thi

            // --- Tiêu đề lớn ---
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH PHÂN CÔNG GIÁM THỊ COI THI - CA " + caThi);
            titleCell.setCellStyle(buildTitleStyle(wb));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));
            titleRow.setHeightInPoints(30);

            // --- Header ---
            Row header = sheet.createRow(1);
            String[] headers = { "STT", "Mã GV", "Họ và Tên", "Vai Trò", "Phòng Thi" };
            CellStyle hStyle = buildHeaderStyle(wb, HEADER_COLOR_PHANCONG);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(hStyle);
            }
            header.setHeightInPoints(20);

            // --- Dữ liệu ---
            CellStyle styleGT1  = buildDataStyle(wb, ROW_COLOR_GT1);
            CellStyle styleGT2  = buildDataStyle(wb, ROW_COLOR_GT2);

            int rowNum = 2;
            for (PhanCongEntry e : entries) {
                Row row = sheet.createRow(rowNum++);
                CellStyle style = e.getVaiTro().contains("1") ? styleGT1 : styleGT2;
                createDataRow(row, style,
                    String.valueOf(e.getStt()),
                    e.getMaCB(),
                    e.getHoTen(),
                    e.getVaiTro(),
                    e.getPhongThi());
            }

            // Tự động lọc
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 4));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * Xuất DANHSACHGIAMSAT.xlsx ra mảng byte
     */
    public static byte[] writeGiamSat(List<GiamSatEntry> entries, int caThi) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Giám Sát Ca " + caThi);

            sheet.setColumnWidth(0, 1500);
            sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 8000);
            sheet.setColumnWidth(3, 7000);

            // Tiêu đề
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH CÁN BỘ GIÁM SÁT HÀNH LANG - CA " + caThi);
            titleCell.setCellStyle(buildTitleStyle(wb));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
            titleRow.setHeightInPoints(30);

            // Header
            Row header = sheet.createRow(1);
            String[] headers = { "STT", "Mã GV", "Họ và Tên", "Khu Vực Giám Sát" };
            CellStyle hStyle = buildHeaderStyle(wb, HEADER_COLOR_GIAMSAT);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(hStyle);
            }
            header.setHeightInPoints(20);

            // Dữ liệu
            CellStyle styleEven = buildDataStyle(wb, ROW_COLOR_EVEN);
            CellStyle styleOdd  = buildDataStyle(wb, ROW_COLOR_GT2);

            int rowNum = 2;
            for (GiamSatEntry e : entries) {
                Row row = sheet.createRow(rowNum++);
                CellStyle style = (e.getStt() % 2 == 0) ? styleEven : styleOdd;
                createDataRow(row, style,
                    String.valueOf(e.getStt()),
                    e.getMaCB(),
                    e.getHoTen(),
                    e.getKhuVuc());
            }

            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 3));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    // ===== Style builders =====

    private static CellStyle buildTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(new XSSFColor(hexToRGB("1F3864"), new org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide[0].getClass().cast(null) == null
            ? new org.apache.poi.xssf.model.IndexedColorMap() {} : null));
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        return s;
    }

    private static CellStyle buildHeaderStyle(XSSFWorkbook wb, String hexColor) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToRGB(hexColor), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setFontHeightInPoints((short) 11);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s);
        return s;
    }

    private static CellStyle buildDataStyle(XSSFWorkbook wb, String hexColor) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToRGB(hexColor), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s);
        return s;
    }

    private static void setBorder(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private static void createDataRow(Row row, CellStyle style, String... values) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i] == null ? "" : values[i]);
            cell.setCellStyle(style);
        }
        row.setHeightInPoints(16);
    }

    private static byte[] hexToRGB(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new byte[]{ (byte) r, (byte) g, (byte) b };
    }
}