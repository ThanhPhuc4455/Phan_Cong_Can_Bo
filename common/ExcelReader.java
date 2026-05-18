package common;

import model.CanBo;
import model.PhongThi;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Đọc file Excel đầu vào: CANBOCOITHI.xlsx và PHONGTHI.xlsx
 * sử dụng thư viện Apache POI.
 *
 * Cấu trúc CANBOCOITHI.xlsx:
 *   Hàng 1: Header (STT | Họ và tên | Ngày sinh | Mã cán bộ | Đơn vị công tác)
 *   Hàng 2+: Dữ liệu
 *
 * Cấu trúc PHONGTHI.xlsx:
 *   Hàng 1: Header (STT | Phòng thi | Địa điểm)
 *   Hàng 2+: Dữ liệu
 */
public class ExcelReader {

    /**
     * Đọc danh sách cán bộ từ file CANBOCOITHI.xlsx
     */
    public static List<CanBo> readCanBo(byte[] fileData) throws IOException {
        List<CanBo> list = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileData))) {
            Sheet sheet = wb.getSheetAt(0);
            boolean firstRow = true;
            int stt = 1;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; } // Bỏ qua header
                if (isRowEmpty(row)) continue;

                // Cột: STT | Mã GV | Họ Tên | Ngày sinh | Đơn vị công tác
                // (dựa theo file thực tế: col0=TT, col1=MãGV, col2=HọTên, col3=NgàySinh, col4=ĐơnVị)
                String maCB    = getCellString(row, 1);
                String hoTen   = getCellString(row, 2);
                String ngaySinh= getCellString(row, 3);
                String donVi   = getCellString(row, 4);

                if (maCB == null || maCB.trim().isEmpty()) continue;

                list.add(new CanBo(stt++, maCB.trim(), hoTen, ngaySinh, donVi));
            }
        }
        return list;
    }

    /**
     * Đọc danh sách phòng thi từ file PHONGTHI.xlsx
     */
    public static List<PhongThi> readPhongThi(byte[] fileData) throws IOException {
        List<PhongThi> list = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileData))) {
            Sheet sheet = wb.getSheetAt(0);
            boolean firstRow = true;
            int stt = 1;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; }
                if (isRowEmpty(row)) continue;

                String tenPhong = getCellString(row, 1);
                String diaDiem  = getCellString(row, 2);

                if (tenPhong == null || tenPhong.trim().isEmpty()) continue;

                list.add(new PhongThi(stt++, tenPhong.trim(), diaDiem));
            }
        }
        return list;
    }

    // ===== Helpers =====

    /** Lấy giá trị ô dưới dạng String (xử lý cả số, ngày, công thức) */
    private static String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Ngày sinh: định dạng dd/MM/yyyy
                    java.util.Date d = cell.getDateCellValue();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                    return sdf.format(d);
                } else {
                    // Số nguyên (mã GV dạng số)
                    double v = cell.getNumericCellValue();
                    if (v == Math.floor(v)) return String.valueOf((long) v);
                    return String.valueOf(v);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue().trim(); }
                catch (Exception e) { return String.valueOf(cell.getNumericCellValue()); }
            default:
                return "";
        }
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}