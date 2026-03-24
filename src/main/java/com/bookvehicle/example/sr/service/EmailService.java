package com.bookvehicle.example.sr.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    public String sendWithdrawOtp(Long userId, String toEmail) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000);

        String key = "withdraw_" + userId;
        otpStorage.put(key, otp);
        otpExpiry.put(key, expiry);

        try {
            sendHtmlEmail(toEmail, "SR-Booking: Ma OTP xac nhan rut tien", buildWithdrawOtpHtml(otp));
        } catch (Exception e) {
            System.err.println("Loi gui email OTP rut tien: " + e.getMessage());
            System.out.println("==================================================");
            System.out.println("OTP RUT TIEN CHO USER " + userId + " LA: " + otp);
            System.out.println("==================================================");
        }

        return null;
    }

    public String verifyWithdrawOtp(Long userId, String otp) {
        String key = "withdraw_" + userId;
        String storedOtp = otpStorage.get(key);
        Long expiry = otpExpiry.get(key);

        if (storedOtp == null || expiry == null) {
            return "Ma OTP khong hop le hoac da het han. Vui long gui lai.";
        }
        if (System.currentTimeMillis() > expiry) {
            otpStorage.remove(key);
            otpExpiry.remove(key);
            return "Ma OTP da het han (5 phut). Vui long gui lai.";
        }
        if (!storedOtp.equals(otp)) {
            return "Ma OTP khong chinh xac.";
        }

        otpStorage.remove(key);
        otpExpiry.remove(key);
        return null;
    }

    public String sendPasswordResetOtp(String toEmail, String otp) {
        try {
            sendHtmlEmail(toEmail, "SR-Booking: Ma OTP dat lai mat khau", buildPasswordResetOtpHtml(otp));
        } catch (Exception e) {
            System.err.println("Loi gui email OTP dat lai mat khau: " + e.getMessage());
            System.out.println("==================================================");
            System.out.println("OTP DAT LAI MAT KHAU CHO " + toEmail + " LA: " + otp);
            System.out.println("==================================================");
        }
        return null;
    }

    public String sendRegistrationOtp(String toEmail, String otp, boolean driverAccount) {
        try {
            String subject = driverAccount
                    ? "SR-Booking: Ma OTP xac thuc dang ky tai xe"
                    : "SR-Booking: Ma OTP xac thuc dang ky tai khoan";
            sendHtmlEmail(toEmail, subject, buildRegistrationOtpHtml(otp, driverAccount));
        } catch (Exception e) {
            System.err.println("Loi gui email OTP dang ky: " + e.getMessage());
            System.out.println("==================================================");
            System.out.println("OTP DANG KY CHO " + toEmail + " LA: " + otp);
            System.out.println("==================================================");
        }
        return null;
    }

    public String sendAdminCreateOtp(String toEmail) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000);

        String key = "create_" + toEmail.toLowerCase();
        otpStorage.put(key, otp);
        otpExpiry.put(key, expiry);

        try {
            sendHtmlEmail(toEmail, "SR-Booking: Ma OTP xac nhan tao tai khoan", buildCreateAccountOtpHtml(otp));
        } catch (Exception e) {
            System.err.println("Loi gui email OTP tao tai khoan: " + e.getMessage());
            System.out.println("==================================================");
            System.out.println("OTP TAO TAI KHOAN CHO " + toEmail + " LA: " + otp);
            System.out.println("==================================================");
        }
        return null;
    }

    public String verifyAdminCreateOtp(String email, String otp) {
        String key = "create_" + email.toLowerCase();
        String storedOtp = otpStorage.get(key);
        Long expiry = otpExpiry.get(key);

        if (storedOtp == null || expiry == null) {
            return "Ma OTP khong hop le hoac da het han. Vui long gui lai.";
        }
        if (System.currentTimeMillis() > expiry) {
            otpStorage.remove(key);
            otpExpiry.remove(key);
            return "Ma OTP da het han (5 phut). Vui long gui lai.";
        }
        if (!storedOtp.equals(otp)) {
            return "Ma OTP khong chinh xac.";
        }
        otpStorage.remove(key);
        otpExpiry.remove(key);
        return null;
    }

    public String checkAdminCreateOtp(String email, String otp) {
        String key = "create_" + email.toLowerCase();
        String storedOtp = otpStorage.get(key);
        Long expiry = otpExpiry.get(key);

        if (storedOtp == null || expiry == null) {
            return "Ma OTP khong hop le hoac da het han. Vui long gui lai.";
        }
        if (System.currentTimeMillis() > expiry) {
            otpStorage.remove(key);
            otpExpiry.remove(key);
            return "Ma OTP da het han (5 phut). Vui long gui lai.";
        }
        if (!storedOtp.equals(otp)) {
            return "Ma OTP khong chinh xac.";
        }
        return null;
    }

    private void sendHtmlEmail(String toEmail, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private String buildCreateAccountOtpHtml(String otp) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8faff;border-radius:16px;">
                <div style="text-align:center;margin-bottom:24px;">
                    <h1 style="color:#0A2463;font-size:24px;margin:0;">SR-Booking</h1>
                    <p style="color:#64748B;font-size:14px;margin:4px 0 0;">Xac nhan tao tai khoan</p>
                </div>
                <div style="background:white;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                    <p style="color:#1E293B;font-size:15px;margin:0 0 8px;">Xin chao,</p>
                    <p style="color:#1E293B;font-size:15px;margin:0 0 16px;">Ma OTP de xac nhan tao tai khoan moi la:</p>
                    <div style="background:#F0F4FF;border-radius:8px;padding:16px;margin:0 auto;display:inline-block;">
                        <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#0A2463;">""" + otp + """
                        </span>
                    </div>
                    <p style="color:#EF4444;font-size:13px;margin:16px 0 0;font-weight:600;">Ma co hieu luc trong 5 phut</p>
                </div>
            </div>
        """;
    }

    private String buildWithdrawOtpHtml(String otp) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8faff;border-radius:16px;">
                <div style="text-align:center;margin-bottom:24px;">
                    <h1 style="color:#0A2463;font-size:24px;margin:0;">SR-Booking</h1>
                    <p style="color:#64748B;font-size:14px;margin:4px 0 0;">Xac nhan rut tien</p>
                </div>
                <div style="background:white;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                    <p style="color:#1E293B;font-size:15px;margin:0 0 16px;">Ma OTP xac nhan rut tien cua ban la:</p>
                    <div style="background:#F0F4FF;border-radius:8px;padding:16px;margin:0 auto;display:inline-block;">
                        <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#0A2463;">""" + otp + """
                        </span>
                    </div>
                    <p style="color:#EF4444;font-size:13px;margin:16px 0 0;font-weight:600;">Ma co hieu luc trong 5 phut</p>
                </div>
            </div>
        """;
    }

    private String buildPasswordResetOtpHtml(String otp) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#f8faff;border-radius:16px;">
                <div style="text-align:center;margin-bottom:24px;">
                    <h1 style="color:#0A2463;font-size:24px;margin:0;">SR-Booking</h1>
                    <p style="color:#64748B;font-size:14px;margin:4px 0 0;">Dat lai mat khau</p>
                </div>
                <div style="background:white;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                    <p style="color:#1E293B;font-size:15px;margin:0 0 8px;">Xin chao,</p>
                    <p style="color:#1E293B;font-size:15px;margin:0 0 16px;">Ma OTP de dat lai mat khau cua ban la:</p>
                    <div style="background:#F0F4FF;border-radius:8px;padding:16px;margin:0 auto;display:inline-block;">
                        <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#0A2463;">""" + otp + """
                        </span>
                    </div>
                    <p style="color:#EF4444;font-size:13px;margin:16px 0 0;font-weight:600;">Ma co hieu luc trong 5 phut</p>
                </div>
            </div>
        """;
    }

    private String buildRegistrationOtpHtml(String otp, boolean driverAccount) {
        String title = driverAccount ? "Xac thuc dang ky tai xe" : "Xac thuc dang ky tai khoan";
        String note = driverAccount
                ? "Sau khi xac thuc email, ho so tai xe cua ban se tiep tuc cho admin duyet."
                : "Sau khi xac thuc email, ban co the dang nhap ngay vao he thong.";

        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:0 auto;padding:32px;background:#f8faff;border-radius:16px;">
                <div style="text-align:center;margin-bottom:24px;">
                    <h1 style="color:#0A2463;font-size:24px;margin:0;">SR-Booking</h1>
                    <p style="color:#64748B;font-size:14px;margin:4px 0 0;">""" + title + """
                    </p>
                </div>
                <div style="background:white;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                    <p style="color:#1E293B;font-size:15px;margin:0 0 8px;">Cam on ban da dang ky tai khoan.</p>
                    <p style="color:#1E293B;font-size:15px;margin:0 0 16px;">Nhap ma OTP sau de xac thuc email cua ban:</p>
                    <div style="background:#F0F4FF;border-radius:8px;padding:16px;margin:0 auto;display:inline-block;">
                        <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#0A2463;">""" + otp + """
                        </span>
                    </div>
                    <p style="color:#EF4444;font-size:13px;margin:16px 0 0;font-weight:600;">Ma co hieu luc trong 5 phut</p>
                    <p style="color:#64748B;font-size:13px;margin:12px 0 0;">""" + note + """
                    </p>
                </div>
            </div>
        """;
    }
}
