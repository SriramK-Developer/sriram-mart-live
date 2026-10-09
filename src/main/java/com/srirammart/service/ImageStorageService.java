package com.srirammart.service;

import com.srirammart.config.AppProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores seller product photos. Every upload is decoded and re-encoded as a JPEG with a random name,
 * so only real images are accepted and nothing user-supplied is ever written to disk as-is.
 */
@Service
public class ImageStorageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_SIDE = 1000;
    private final Path dir;

    public ImageStorageService(AppProperties props) throws IOException {
        this.dir = Paths.get(props.getUploadsDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
    }

    /** @return public path such as /uploads/abc.jpg, or null when no file was chosen. */
    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Image must be smaller than 5 MB.");
        BufferedImage img;
        try (InputStream in = file.getInputStream()) { img = ImageIO.read(in); }
        if (img == null) throw new IllegalArgumentException("Upload a valid JPG, PNG or GIF image.");
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(img.getWidth(), img.getHeight()));
        int w = Math.max(1, (int) Math.round(img.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(img.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        String name = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        ImageIO.write(out, "jpg", dir.resolve(name).toFile());
        return "/uploads/" + name;
    }
}
