package com.bookvehicle.example.sr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // In-memory OTP storage: key = "withdraw_" + userId
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    /**
     * Gửi OTP 6 chữ số qua email để xác nhận rút tiền.
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String sendWithdrawOtp(Long userId, String toEmail) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 phút

        String key = "withdraw_" + userId;
        otpStorage.put(key, otp);
        otpExpiry.put(key, expiry);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("SR-Booking: Mã OTP xác nhận rút tiền");
            helper.setText(buildOtpEmailHtml(otp), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email OTP: " + e.getMessage());
            // Fallback: in OTP ra console
            System.out.println("==================================================");
            System.out.println("OTP RÚT TIỀN CHO USER " + userId + " LÀ: " + otp);
            System.out.println("==================================================");
        }

        return null;
    }

    /**
     * Xác minh OTP rút tiền.
     * @return null nếu hợp lệ, chuỗi lỗi nếu sai/hết hạn.
     */
    public String verifyWithdrawOtp(Long userId, String otp) {
        String key = "withdraw_" + userId;
        String storedOtp = otpStorage.get(key);
        Long expiry = otpExpiry.get(key);

        if (storedOtp == null || expiry == null) {
            return "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại.";
        }
        if (System.currentTimeMillis() > expiry) {
            otpStorage.remove(key);
            otpExpiry.remove(key);
            return "Mã OTP đã hết hạn (5 phút). Vui lòng gửi lại.";
        }
        if (!storedOtp.equals(otp)) {
            return "Mã OTP không chính xác.";
        }

        // OTP hợp lệ → xóa
        otpStorage.remove(key);
        otpExpiry.remove(key);
        return null;
    }

    /**
     * Gửi OTP đặt lại mật khẩu qua email.
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String sendPasswordResetOtp(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("SR-Booking: Mã OTP đặt lại mật khẩu");
            helper.setText(buildPasswordResetOtpHtml(otp), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email OTP đặt lại mật khẩu: " + e.getMessage());
            // Fallback: in OTP ra console
            System.out.println("==================================================");
            System.out.println("OTP ĐẶT LẠI MẬT KHẨU CHO " + toEmail + " LÀ: " + otp);
            System.out.println("==================================================");
        }
        return null;
    }

    /**
     * Gửi OTP xác nhận tạo tài khoản (admin tạo user mới).
     */
    public String sendAdminCreateOtp(String toEmail) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000);

        String key = "create_" + toEmail.toLowerCase();
        otpStorage.put(key, otp);
        otpExpiry.put(key, expiry);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("SR-Booking: Mã OTP xác nhận tạo tài khoản");
            helper.setText(buildCreateAccountOtpHtml(otp), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email OTP tạo tài khoản: " + e.getMessage());
            System.out.println("==================================================");
            System.out.println("OTP TẠO TÀI KHOẢN CHO " + toEmail + " LÀ: " + otp);
            System.out.println("==================================================");
        }
        return null;
    }

    /**
     * Xác minh OTP tạo tài khoản.
     */
    public String verifyAdminCreateOtp(String email, String otp) {
        String key = "create_" + email.toLowerCase();
        String storedOtp = otpStorage.get(key);
        Long expiry = otpExpiry.get(key);

        if (storedOtp == null || expiry == null) {
            return "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại.";
        }
        if (System.currentTimeMillis() > expiry) {
            otpStorage.remove(key);
            otpExpiry.remove(key);
            return "Mã OTP đã hết hạn (5 phút). Vui lòng gửi lại.";
        }
        if (!storedOtp.equals(otp)) {
            return "Mã OTP không chính xác.";
        }
        otpStorage.remove(key);
        otpExpiry.remove(key);
        return null;
    }

    /**
     * Kiểm tra OTP tạo tài khoản (không xóa – dùng cho AJAX pre-check).
     */
    public String checkAdminCreateOtp(String email, String otp) {
        String key = "create_" + email.toLowerCase();
        String storedOtp = otpStorage.get(key);
        Long expiry = otpExpiry.get(key);

        if (storedOtp == null || expiry == null) {
            return "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại.";
        }
        if (System.currentTimeMillis() > expiry) {
            otpStorage.remove(key);
            otpExpiry.remove(key);
            return "Mã OTP đã hết hạn (5 phút). Vui lòng gửi lại.";
        }
        if (!storedOtp.equals(otp)) {
            return "Mã OTP không chính xác.";
        }
        return null;
    }

    private String buildCreateAccountOtpHtml(String otp) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8faff;border-radius:16px;">
                <div style="text-align:center;margin-bottom:24px;">
                    <h1 style="color:#0A2463;font-size:24px;margin:0;">SR-Booking</h1>
                    <p style="color:#64748B;font-size:14px;margin:4px 0 0;">Xác nhận tạo tài khoản</p>
                </div>
                <div style="background:white;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                    <p style="color:#1E293B;font-size:15px;margin:0 0 8px;">Xin chào,</p>
                    <p style="color:#1E293B;font-size:15px;margin:0 0 16px;">Mã OTP để xác nhận tạo tài khoản mới là:</p>
                    <div style="background:#F0F4FF;border-radius:8px;padding:16px;margin:0 auto;display:inline-block;">
                        <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#0A2463;">""" + otp + """
                        </span>
                    </div>
                    <p style="color:#EF4444;font-size:13px;margin:16px 0 0;font-weight:600;">⏱ Mã có hiệu lực trong 5 phút</p>
                </div>
                <p style="color:#94A3B8;font-size:12px;text-align:center;margin:20px 0 0;">
                    Email này được gửi từ hệ thống SR-Booking Admin.
                </p>
            </div>
        """;
    }

    private String buildOtpEmailHtml(String otp) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8faff;border-radius:16px;">
                <div style="text-align:center;margin-bottom:24px;">
                    <h1 style="color:#0A2463;font-size:24px;margin:0;">SR-Booking</h1>
                    <p style="color:#64748B;font-size:14px;margin:4px 0 0;">Xác nhận rút tiền</p>
                </div>
                <div style="background:white;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                    <p style="color:#1E293B;font-size:15px;margin:0 0 16px;">Mã OTP xác nhận rút tiền của bạn là:</p>
                    <div style="background:#F0F4FF;border-radius:8px;padding:16px;margin:0 auto;display:inline-block;">
                        <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#0A2463;">""" + otp + """
                        </span>
                    </div>
                    <p style="color:#EF4444;font-size:13px;margin:16px 0 0;font-weight:600;">⏱ Mã có hiệu lực trong 5 phút</p>
                </div>
                <p style="color:#94A3B8;font-size:12px;text-align:center;margin:20px 0 0;">
                    Nếu bạn không yêu cầu rút tiền, vui lòng bỏ qua email này.
                </p>
            </div>
        """;
    }

    private String buildPasswordResetOtpHtml(String otp) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8faff;border-radius:16px;">
                <div style="text-align:center;margin-bottom:24px;">
                    <h1 style="color:#0A2463;font-size:24px;margin:0;">SR-Booking</h1>
                    <p style="color:#64748B;font-size:14px;margin:4px 0 0;">Đặt lại mật khẩu</p>
                </div>
                <div style="background:white;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                    <p style="color:#1E293B;font-size:15px;margin:0 0 8px;">Xin chào,</p>
                    <p style="color:#1E293B;font-size:15px;margin:0 0 16px;">Mã OTP để đặt lại mật khẩu của bạn là:</p>
                    <div style="background:#F0F4FF;border-radius:8px;padding:16px;margin:0 auto;display:inline-block;">
                        <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#0A2463;">""" + otp + """
                        </span>
                    </div>
                    <p style="color:#EF4444;font-size:13px;margin:16px 0 0;font-weight:600;">⏱ Mã có hiệu lực trong 5 phút</p>
                    <p style="color:#64748B;font-size:13px;margin:12px 0 0;">Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                </div>
                <p style="color:#94A3B8;font-size:12px;text-align:center;margin:20px 0 0;">
                    Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                </p>
            </div>
        """;
    }
}
