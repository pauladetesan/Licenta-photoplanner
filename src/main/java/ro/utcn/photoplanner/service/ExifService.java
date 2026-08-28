package ro.utcn.photoplanner.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Citește coordonatele GPS din metadatele EXIF ale unei fotografii încărcate. */
@Service
public class ExifService {

    public CoordonateFoto extrageCoordonate(MultipartFile fisier) {
        if (fisier == null || fisier.isEmpty()) {
            return CoordonateFoto.negasit("Nicio fotografie selectată.");
        }

        Metadata metadata;
        try {
            metadata = ImageMetadataReader.readMetadata(fisier.getInputStream());
        } catch (ImageProcessingException | IOException e) {
            return CoordonateFoto.negasit("Fișierul nu a putut fi citit ca imagine.");
        }

        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory == null) {
            return CoordonateFoto.negasit("Fotografia nu conține date GPS în metadate.");
        }

        GeoLocation locatie = gpsDirectory.getGeoLocation();
        if (locatie == null || locatie.isZero()) {
            return CoordonateFoto.negasit("Fotografia nu conține date GPS în metadate.");
        }

        Integer orientare = null;
        Double directie = gpsDirectory.getDoubleObject(GpsDirectory.TAG_IMG_DIRECTION);
        if (directie != null) {
            orientare = (int) Math.round(directie) % 360;
        }

        return CoordonateFoto.gasit(locatie.getLatitude(), locatie.getLongitude(), orientare);
    }
}
