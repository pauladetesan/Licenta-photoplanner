package ro.utcn.photoplanner.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Decodarea imaginilor și generarea miniaturilor. */
@Service
public class ImagineService {

    private static final Logger log = LoggerFactory.getLogger(ImagineService.class);

    /** Latura maximă a miniaturii, în pixeli. */
    private static final int LATURA_MINIATURA = 400;

    /**
     * Decodează imaginea și o rotește după eticheta EXIF de orientare.
     * <p>
     * Telefoanele salvează de obicei fotografia în orientarea senzorului și notează separat
     * cum ar trebui rotită. Fără pasul ăsta, miniaturile pozelor verticale ies culcate.
     *
     * @return imaginea decodată, sau null dacă fișierul nu e o imagine pe care o putem citi
     */
    public BufferedImage decodeaza(byte[] continut) {
        BufferedImage imagine;
        try {
            imagine = ImageIO.read(new ByteArrayInputStream(continut));
        } catch (IOException e) {
            return null;
        }
        if (imagine == null) {
            return null;
        }
        return roteste(imagine, orientareExif(continut));
    }

    /** Citește eticheta EXIF de orientare (1–8). Întoarce 1 (normal) dacă lipsește. */
    private int orientareExif(byte[] continut) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(continut));
            ExifIFD0Directory director = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (director != null && director.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return director.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception e) {
            log.debug("Nu am putut citi orientarea EXIF: {}", e.toString());
        }
        return 1;
    }

    /** Aplică rotația cerută de eticheta EXIF. Variantele oglindite (2, 4, 5, 7) sunt lăsate ca atare. */
    private BufferedImage roteste(BufferedImage sursa, int orientare) {
        int grade = switch (orientare) {
            case 3 -> 180;
            case 6 -> 90;
            case 8 -> 270;
            default -> 0;
        };
        if (grade == 0) {
            return sursa;
        }

        boolean peLat = grade == 90 || grade == 270;
        int latime = peLat ? sursa.getHeight() : sursa.getWidth();
        int inaltime = peLat ? sursa.getWidth() : sursa.getHeight();

        BufferedImage rezultat = new BufferedImage(latime, inaltime, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rezultat.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.translate(latime / 2.0, inaltime / 2.0);
        g.rotate(Math.toRadians(grade));
        g.drawImage(sursa, -sursa.getWidth() / 2, -sursa.getHeight() / 2, null);
        g.dispose();
        return rezultat;
    }

    /** Creează o miniatură JPEG cu latura lungă de cel mult {@value #LATURA_MINIATURA} px. */
    public byte[] miniatura(BufferedImage imagine) {
        double scara = (double) LATURA_MINIATURA
                / Math.max(imagine.getWidth(), imagine.getHeight());
        if (scara > 1.0) {
            scara = 1.0; // imaginile mici rămân la dimensiunea lor
        }

        int latime = Math.max(1, (int) Math.round(imagine.getWidth() * scara));
        int inaltime = Math.max(1, (int) Math.round(imagine.getHeight() * scara));

        BufferedImage mica = new BufferedImage(latime, inaltime, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = mica.createGraphics();
        // Fundal alb: JPEG nu are transparență, altfel zonele transparente ies negre.
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, latime, inaltime);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(imagine, 0, 0, latime, inaltime, null);
        g.dispose();

        try (ByteArrayOutputStream iesire = new ByteArrayOutputStream()) {
            ImageIO.write(mica, "jpg", iesire);
            return iesire.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Nu pot genera miniatura", e);
        }
    }
}
