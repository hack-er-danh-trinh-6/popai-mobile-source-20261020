package com.health.pop.data.local

import com.health.pop.domain.model.AssessmentGroup
import com.health.pop.domain.model.AssessmentQuestion

object MockDataProvider {
    // Bộ câu hỏi là nội dung tĩnh của sản phẩm; không chứa hồ sơ hoặc kết quả người dùng mẫu.
    val assessmentQuestions: List<AssessmentQuestion> = listOf(
        // VN: Thói quen xem video ngắn
        AssessmentQuestion("VN1", AssessmentGroup.VN, "Bạn có hay lướt xem video ngắn (TikTok/Reels/Shorts) khi nghỉ giải lao?"),
        AssessmentQuestion("VN2", AssessmentGroup.VN, "Bạn có thường xuyên quẹt chuyển video chỉ sau 3-5 giây đầu tiên?"),
        AssessmentQuestion("VN3", AssessmentGroup.VN, "Bạn có cảm thấy khó dừng lướt video dù đã đến giờ vào học hoặc đi ngủ?"),
        AssessmentQuestion("VN4", AssessmentGroup.VN, "Bạn có lướt video ngắn trong lúc ăn uống, đi lại hoặc chờ xe?"),
        AssessmentQuestion("VN5", AssessmentGroup.VN, "Sau khi lướt video ngắn 20 phút, bạn cảm thấy đầu óc mệt mỏi nhưng vẫn khó buông máy?"),

        // PT: Phụ thuộc thiết bị số
        AssessmentQuestion("PT1", AssessmentGroup.PT, "Bạn có thói quen cầm điện thoại lên ngay lập tức khi không có việc gì làm?"),
        AssessmentQuestion("PT2", AssessmentGroup.PT, "Bạn có cảm thấy bồn chồn nếu không mang theo điện thoại bên mình?"),
        AssessmentQuestion("PT3", AssessmentGroup.PT, "Bạn có kiểm tra màn hình điện thoại liên tục dù không có chuông báo?"),
        AssessmentQuestion("PT4", AssessmentGroup.PT, "Màn hình đầu tiên bạn nhìn thấy sau khi thức dậy là điện thoại?"),
        AssessmentQuestion("PT5", AssessmentGroup.PT, "Bạn cảm thấy khó chịu khi môi trường xung quanh không có mạng internet?"),

        // PB: Biểu hiện phân mảnh chú ý
        AssessmentQuestion("PB1", AssessmentGroup.PB, "Bạn có thấy khó đọc hết một trang sách/bài đọc dài mà không bị xao nhãng?"),
        AssessmentQuestion("PB2", AssessmentGroup.PB, "Khi đang học bài, suy nghĩ của bạn có hay nhảy sang các chủ đề khác?"),
        AssessmentQuestion("PB3", AssessmentGroup.PB, "Bạn có thấy sốt ruột khi phải suy nghĩ giải một bài tập đòi hỏi nhiều bước?"),
        AssessmentQuestion("PB4", AssessmentGroup.PB, "Bạn có hay mở điện thoại để làm một việc rồi quên mất và lướt ứng dụng khác?"),
        AssessmentQuestion("PB5", AssessmentGroup.PB, "Bạn cảm thấy sự kiên nhẫn của mình giảm đi so với trước đây?"),

        // TT: Khả năng tập trung sâu
        AssessmentQuestion("TT1", AssessmentGroup.TT, "Bạn có thể duy trì học tập 25 phút liên tục mà không đụng vào điện thoại?"),
        AssessmentQuestion("TT2", AssessmentGroup.TT, "Khi gặp bài tập khó, bạn sẵn sàng kiên trì tìm cách tự suy nghĩ giải quyết?"),
        AssessmentQuestion("TT3", AssessmentGroup.TT, "Bạn có thói quen chủ động sắp xếp thứ tự ưu tiên cho bài học trong ngày?"),
        AssessmentQuestion("TT4", AssessmentGroup.TT, "Bạn cảm thấy hài lòng khi hoàn thành trọn vẹn một khoảng thời gian học tập?"),
        AssessmentQuestion("TT5", AssessmentGroup.TT, "Bạn chủ động cất điện thoại ra xa tầm tay trước khi bắt đầu ngồi học?")
    )
}
