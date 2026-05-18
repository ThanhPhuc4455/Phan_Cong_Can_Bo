package model;

/**
 * Model: Một dòng trong bảng phân công giám sát hành lang
 */
public class GiamSatEntry {
    private int    stt;
    private String maCB;
    private String hoTen;
    private String khuVuc;  // VD: "Từ C101 đến C110"

    public GiamSatEntry() {}

    public GiamSatEntry(int stt, String maCB, String hoTen, String khuVuc) {
        this.stt    = stt;
        this.maCB   = maCB;
        this.hoTen  = hoTen;
        this.khuVuc = khuVuc;
    }

    public int    getStt()             { return stt; }
    public void   setStt(int s)        { this.stt = s; }
    public String getMaCB()            { return maCB; }
    public void   setMaCB(String m)    { this.maCB = m; }
    public String getHoTen()           { return hoTen; }
    public void   setHoTen(String h)   { this.hoTen = h; }
    public String getKhuVuc()          { return khuVuc; }
    public void   setKhuVuc(String k)  { this.khuVuc = k; }
}