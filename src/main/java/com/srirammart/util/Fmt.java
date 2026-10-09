package com.srirammart.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Template helper, used in views as ${@fmt.inr(value)}. */
@Component("fmt")
public class Fmt {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);

    /** Indian rupee formatting with lakh/crore grouping, e.g. 125000 -> ₹1,25,000 */
    public String inr(Number n) {
        if (n == null) return "₹0";
        BigDecimal bd = n instanceof BigDecimal ? (BigDecimal) n : BigDecimal.valueOf(n.doubleValue());
        boolean neg = bd.signum() < 0;
        bd = bd.abs();
        boolean hasPaise = bd.remainder(BigDecimal.ONE).signum() != 0;
        bd = bd.setScale(hasPaise ? 2 : 0, RoundingMode.HALF_UP);
        String plain = bd.toPlainString();
        String whole = plain, frac = "";
        int dot = plain.indexOf('.');
        if (dot >= 0) { whole = plain.substring(0, dot); frac = plain.substring(dot); }
        String grouped;
        if (whole.length() <= 3) grouped = whole;
        else {
            String last3 = whole.substring(whole.length() - 3);
            String rest = whole.substring(0, whole.length() - 3).replaceAll("\\B(?=(\\d{2})+(?!\\d))", ",");
            grouped = rest + "," + last3;
        }
        return (neg ? "-₹" : "₹") + grouped + frac;
    }

    /** 2400 -> 2.4k */
    public String count(Number n) {
        if (n == null) return "0";
        long v = n.longValue();
        if (v < 1000) return String.valueOf(v);
        double k = v / 1000.0;
        String s = String.format(Locale.ENGLISH, "%.1f", k);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s + "k";
    }

    public String dateTime(LocalDateTime t) { return t == null ? "" : t.format(DATE_TIME); }
    public String date(LocalDateTime t) { return t == null ? "" : t.format(DATE); }
    public String date(LocalDate d) { return d == null ? "" : d.format(DATE); }
    public String day(LocalDate d) { return d == null ? "" : d.format(DAY); }

    public String ago(LocalDateTime t) {
        if (t == null) return "";
        Duration d = Duration.between(t, LocalDateTime.now());
        long m = d.toMinutes();
        if (m < 1) return "just now";
        if (m < 60) return m + (m == 1 ? " minute ago" : " minutes ago");
        long h = d.toHours();
        if (h < 24) return h + (h == 1 ? " hour ago" : " hours ago");
        long days = d.toDays();
        if (days < 7) return days + (days == 1 ? " day ago" : " days ago");
        if (days < 30) { long w = days / 7; return w + (w == 1 ? " week ago" : " weeks ago"); }
        return t.format(DATE);
    }

    /** Stars helper: value 0..5 -> percentage width for the star mask. */
    public int starPct(Number rating) {
        if (rating == null) return 0;
        double r = Math.max(0, Math.min(5, rating.doubleValue()));
        return (int) Math.round(r / 5.0 * 100);
    }

    public String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        String s = p[0].substring(0, 1);
        if (p.length > 1) s += p[p.length - 1].substring(0, 1);
        return s.toUpperCase(Locale.ENGLISH);
    }
}
