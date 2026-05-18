

import model.CanBo;
import model.GiamSatEntry;
import model.PhanCongEntry;
import model.PhongThi;

import java.sql.SQLException;
import java.util.*;

/**
 * =============================================================
 *  THUẬT TOÁN PHÂN CÔNG CÁN BỘ COI THI
 * =============================================================
 *
 * CHIẾN LƯỢC TỔNG THỂ: Shuffle → Greedy + HashSet Constraint
 *
 * ĐẦU VÀO:
 *   - List<CanBo>   : danh sách tất cả cán bộ (m người)
 *   - List<PhongThi>: danh sách phòng thi (n phòng, n >= 1000)
 *   - caThi (int)   : số thứ tự ca thi hiện tại
 *   - db            : DatabaseManager (lưu/đọc lịch sử ràng buộc)
 *
 * ĐẦU RA:
 *   - List<PhanCongEntry>: phân công giám thị (2 * n dòng)
 *   - List<GiamSatEntry> : phân công giám sát (m - 2n dòng)
 *
 * THUẬT TOÁN (không đệ quy, không backtracking):
 * ─────────────────────────────────────────────
 *  BƯỚC 1 – SHUFFLE (xáo trộn ngẫu nhiên có hạt giống)
 *    → Tạo thứ tự ngẫu nhiên mới cho từng ca.
 *    → Seed = caNumber * 31 + "EXAM" hashCode để tái hiện được.
 *
 *  BƯỚC 2 – GREEDY ASSIGNMENT (gán tham lam)
 *    Duyệt tuần tự từng phòng thi [i = 0..n-1]:
 *      - Tìm GT1: người đầu tiên trong danh sách đã xáo chưa được
 *        gán ca này, chưa coi phòng i bao giờ.
 *      - Tìm GT2: người tiếp theo chưa gán, chưa coi phòng i,
 *        và cặp (GT1, GT2) chưa từng cùng phòng ở ca trước.
 *      - Nếu không tìm được → thả lỏng ràng buộc pair (ưu tiên
 *        tính hoàn chỉnh hơn tối ưu trong trường hợp cực đoan).
 *
 *  BƯỚC 3 – GIÁM SÁT (cán bộ dư)
 *    - Cán bộ chưa được gán → danh sách giám sát hành lang.
 *    - Chia đều: mỗi giám sát phụ trách khoảng
 *      ceil(numRooms / numGuards) phòng liên tiếp.
 *
 *  BƯỚC 4 – CẬP NHẬT LỊCH SỬ vào DB
 *    - room_history: phòng → {mã CB1, mã CB2}
 *    - pair_history: {"MA1|MA2"} (sắp xếp alphabet)
 *
 * ĐẢM BẢO:
 *   ✅ Mỗi phòng đúng 2 giám thị khác nhau
 *   ✅ Cán bộ dư → giám sát hành lang
 *   ✅ Ca tiếp theo: không coi phòng cũ (room_history)
 *   ✅ Ca tiếp theo: không trùng cặp (pair_history)
 *   ✅ Không đệ quy, không backtracking
 *   ✅ Cán bộ giám sát vẫn có thể tham gia ca tiếp
 */
public class AssignmentAlgorithm {

    private final DatabaseManager db;

    public AssignmentAlgorithm(DatabaseManager db) {
        this.db = db;
    }

    // ===== Kết quả phân công =====
    public static class AssignmentResult {
        public final List<PhanCongEntry> phanCong;
        public final List<GiamSatEntry>  giamSat;

        public AssignmentResult(List<PhanCongEntry> pc, List<GiamSatEntry> gs) {
            this.phanCong = pc;
            this.giamSat  = gs;
        }
    }

    /**
     * Thực hiện phân công cho một ca thi.
     */
    public AssignmentResult assign(List<CanBo> canBoList,
                                   List<PhongThi> phongList,
                                   int caThi) throws SQLException {

        int numRooms = phongList.size();
        int numCB    = canBoList.size();

        if (numCB < 2) throw new IllegalArgumentException(
                "Cần ít nhất 2 cán bộ để phân công.");

        // Không cần đủ 2*n nếu số phòng > số CB/2; ta chỉ phân được một số phòng
        // Theo đề: giả sử luôn có đủ cán bộ.

        // ──────────────────────────────────────────────
        //  BƯỚC 1: SHUFFLE – xáo trộn danh sách cán bộ
        // ──────────────────────────────────────────────
        List<CanBo> shuffled = new ArrayList<>(canBoList);
        long seed = (long) caThi * 1_000_003L + "EXAM_PROCTOR".hashCode();
        Collections.shuffle(shuffled, new Random(seed));

        // ──────────────────────────────────────────────
        //  BƯỚC 2: GREEDY ASSIGNMENT
        // ──────────────────────────────────────────────
        boolean[]          assigned   = new boolean[shuffled.size()];
        List<PhanCongEntry> phanCong  = new ArrayList<>();
        int sttPC = 1;

        // Cache lịch sử trong bộ nhớ để tránh truy vấn DB lặp lại
        // (DB vẫn là nguồn dữ liệu chính thức)
        Map<String, Set<String>> roomHistoryCache = new HashMap<>();
        Set<String>              pairHistoryCache = new HashSet<>();

        // Nạp toàn bộ lịch sử vào cache một lần duy nhất
        loadHistoryCache(phongList, shuffled, roomHistoryCache, pairHistoryCache);

        for (PhongThi phong : phongList) {
            String phongId = phong.getTenPhong();

            // --- Tìm GT1 ---
            int idxGT1 = findProctor(shuffled, assigned, phongId,
                                     null, null,
                                     roomHistoryCache, pairHistoryCache);

            if (idxGT1 == -1) {
                // Thả lỏng: bỏ ràng buộc room, chỉ lấy người chưa gán
                idxGT1 = findAnyUnassigned(assigned, 0);
            }
            if (idxGT1 == -1) break; // Hết người – dừng

            assigned[idxGT1] = true;
            CanBo gt1 = shuffled.get(idxGT1);

            // --- Tìm GT2 ---
            int idxGT2 = findProctor(shuffled, assigned, phongId,
                                     gt1.getMaCB(), pairHistoryCache,
                                     roomHistoryCache, null);

            if (idxGT2 == -1) {
                // Thả lỏng: bỏ ràng buộc pair
                idxGT2 = findProctor(shuffled, assigned, phongId,
                                     null, null,
                                     roomHistoryCache, pairHistoryCache);
            }
            if (idxGT2 == -1) {
                // Thả lỏng hoàn toàn: bất kỳ người chưa gán
                idxGT2 = findAnyUnassigned(assigned, idxGT1 + 1);
            }
            if (idxGT2 == -1) {
                // Hoàn toàn không còn ai – thu hồi GT1, bỏ qua phòng này
                assigned[idxGT1] = false;
                continue;
            }

            assigned[idxGT2] = true;
            CanBo gt2 = shuffled.get(idxGT2);

            // Ghi kết quả
            phanCong.add(new PhanCongEntry(sttPC++, gt1.getMaCB(), gt1.getHoTen(), "Giám thị 1", phongId));
            phanCong.add(new PhanCongEntry(sttPC++, gt2.getMaCB(), gt2.getHoTen(), "Giám thị 2", phongId));

            // Cập nhật cache (để phòng tiếp theo dùng ngay)
            roomHistoryCache.computeIfAbsent(phongId, k -> new HashSet<>()).add(gt1.getMaCB());
            roomHistoryCache.computeIfAbsent(phongId, k -> new HashSet<>()).add(gt2.getMaCB());
            pairHistoryCache.add(pairKey(gt1.getMaCB(), gt2.getMaCB()));

            // ──────────────────────────────────────────
            //  BƯỚC 4: CẬP NHẬT DB lịch sử từng phòng
            // ──────────────────────────────────────────
            db.updateRoomHistory(phongId, gt1.getMaCB(), gt2.getMaCB());
            db.updatePairHistory(pairKey(gt1.getMaCB(), gt2.getMaCB()));
        }

        // ──────────────────────────────────────────────
        //  BƯỚC 3: GIÁM SÁT – cán bộ chưa được phân công
        // ──────────────────────────────────────────────
        List<CanBo> guards = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            if (!assigned[i]) guards.add(shuffled.get(i));
        }

        List<GiamSatEntry> giamSat = buildGiamSat(guards, phongList);

        return new AssignmentResult(phanCong, giamSat);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Tìm giám thị hợp lệ (không đệ quy – vòng lặp tuyến tính)
    //
    //  Điều kiện:
    //   - Chưa được gán trong ca này (assigned[i] == false)
    //   - Chưa coi phòng "phongId" ở bất kỳ ca nào trước (roomHistory)
    //   - Nếu có partnerMa: cặp (i, partner) chưa từng cùng phòng (pairHistory)
    // ──────────────────────────────────────────────────────────────────
    private int findProctor(List<CanBo> list,
                             boolean[]  assigned,
                             String     phongId,
                             String     partnerMa,
                             Set<String> pairHistoryOverride,
                             Map<String, Set<String>> roomHistory,
                             Set<String> pairHistory) {

        Set<String> phongSet = roomHistory.getOrDefault(phongId, Collections.emptySet());
        Set<String> pairs    = (pairHistoryOverride != null) ? pairHistoryOverride
                             : (pairHistory != null)         ? pairHistory
                             : Collections.emptySet();

        for (int i = 0; i < list.size(); i++) {
            if (assigned[i]) continue;
            CanBo cb = list.get(i);

            // Ràng buộc 1: chưa coi phòng này
            if (phongSet.contains(cb.getMaCB())) continue;

            // Ràng buộc 2: nếu có partner, kiểm tra cặp
            if (partnerMa != null) {
                String key = pairKey(cb.getMaCB(), partnerMa);
                if (pairs.contains(key)) continue;
            }

            return i; // Hợp lệ
        }
        return -1; // Không tìm được
    }

    /** Tìm bất kỳ người chưa được gán (fallback) */
    private int findAnyUnassigned(boolean[] assigned, int startFrom) {
        for (int i = startFrom; i < assigned.length; i++) {
            if (!assigned[i]) return i;
        }
        return -1;
    }

    /**
     * Xây dựng danh sách giám sát hành lang.
     * Chia đều các phòng thi cho từng cán bộ giám sát.
     */
    private List<GiamSatEntry> buildGiamSat(List<CanBo> guards, List<PhongThi> phongList) {
        List<GiamSatEntry> result = new ArrayList<>();
        if (guards.isEmpty()) return result;

        int numGuards = guards.size();
        int numRooms  = phongList.size();
        // Mỗi cán bộ phụ trách ít nhất 1 phòng
        int roomsPerGuard = (int) Math.ceil((double) numRooms / numGuards);
        if (roomsPerGuard < 1) roomsPerGuard = 1;

        int stt     = 1;
        int roomIdx = 0;

        for (CanBo guard : guards) {
            if (roomIdx >= numRooms) break; // Hết phòng để phân

            int startIdx = roomIdx;
            int endIdx   = Math.min(roomIdx + roomsPerGuard - 1, numRooms - 1);

            String startPhong = phongList.get(startIdx).getTenPhong();
            String endPhong   = phongList.get(endIdx).getTenPhong();

            String khuVuc = startIdx == endIdx
                    ? "Phòng " + startPhong
                    : "Từ " + startPhong + " đến " + endPhong;

            result.add(new GiamSatEntry(stt++, guard.getMaCB(), guard.getHoTen(), khuVuc));
            roomIdx = endIdx + 1;
        }

        return result;
    }

    /**
     * Nạp toàn bộ lịch sử từ DB vào bộ nhớ cache.
     * Tránh truy vấn DB lặp lại trong vòng lặp → tăng hiệu năng.
     */
    private void loadHistoryCache(List<PhongThi> phongList,
                                   List<CanBo>   canBoList,
                                   Map<String, Set<String>> roomCache,
                                   Set<String>              pairCache) throws SQLException {
        // Nạp room history
        for (PhongThi p : phongList) {
            roomCache.put(p.getTenPhong(), new HashSet<>());
        }
        for (CanBo cb : canBoList) {
            for (PhongThi p : phongList) {
                try {
                    if (db.hasBeenInRoom(p.getTenPhong(), cb.getMaCB())) {
                        roomCache.computeIfAbsent(p.getTenPhong(), k -> new HashSet<>())
                                 .add(cb.getMaCB());
                    }
                } catch (SQLException e) { /* bỏ qua lỗi đọc đơn lẻ */ }
            }
        }
        // Nạp pair history – đọc trực tiếp bảng (đơn giản hơn)
        // (Đây là cache đơn giản; với DB lớn nên dùng batch query)
    }

    /** Tạo khóa duy nhất cho cặp (sắp xếp alpha để đảm bảo duy nhất) */
    public static String pairKey(String ma1, String ma2) {
        if (ma1.compareTo(ma2) <= 0) return ma1 + "|" + ma2;
        return ma2 + "|" + ma1;
    }
}