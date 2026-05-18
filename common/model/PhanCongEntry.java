package model;
/**
 * Model: Một dòng trong bảng phân công giám thị
 */
public class PhanCongEntry {
    private int    stt;
    private String maCB;
    private String hoTen;
    private String vaiTro;   // "Giám thị 1" hoặc "Giám thị 2"
    private String phongThi; // Tên phòng thi

    public PhanCongEntry() {}

    public PhanCongEntry(int stt, String maCB, String hoTen, String vaiTro, String phongThi) {
        this.stt      = stt;
        this.maCB     = maCB;
        this.hoTen    = hoTen;
        this.vaiTro   = vaiTro;
        this.phongThi = phongThi;
    }

    public int    getStt()              { return stt; }
    public void   setStt(int s)         { this.stt = s; }
    public String getMaCB()             { return maCB; }
    public void   setMaCB(String m)     { this.maCB = m; }
    public String getHoTen()            { return hoTen; }
    public void   setHoTen(String h)    { this.hoTen = h; }
    public String getVaiTro()           { return vaiTro; }
    public void   setVaiTro(String v)   { this.vaiTro = v; }
    public String getPhongThi()         { return phongThi; }
    public void   setPhongThi(String p) { this.phongThi = p; }
}