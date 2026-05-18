package common.model;
import java.io.Serializable;

/**
 * Model: Cán bộ coi thi
 */
public class CanBo implements Serializable {
    private int stt;
    private String maCB;    // Mã cán bộ (VD: GV01, 105150103)
    private String hoTen;   // Họ và tên
    private String ngaySinh;// Ngày sinh (dạng chuỗi)
    private String donVi;   // Đơn vị công tác

    public CanBo() {}

    public CanBo(int stt, String maCB, String hoTen, String ngaySinh, String donVi) {
        this.stt     = stt;
        this.maCB    = maCB;
        this.hoTen   = hoTen;
        this.ngaySinh= ngaySinh;
        this.donVi   = donVi;
    }

    // ---- Getters / Setters ----
    public int    getStt()      { return stt; }
    public void   setStt(int s) { this.stt = s; }

    public String getMaCB()         { return maCB; }
    public void   setMaCB(String m) { this.maCB = m; }

    public String getHoTen()          { return hoTen; }
    public void   setHoTen(String h)  { this.hoTen = h; }

    public String getNgaySinh()          { return ngaySinh; }
    public void   setNgaySinh(String n)  { this.ngaySinh = n; }

    public String getDonVi()          { return donVi; }
    public void   setDonVi(String d)  { this.donVi = d; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", maCB, hoTen, donVi);
    }
}