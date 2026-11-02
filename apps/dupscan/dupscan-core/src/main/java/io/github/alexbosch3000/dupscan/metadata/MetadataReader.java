package io.github.alexbosch3000.dupscan.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Best-effort EXIF capture-date reader. Never throws on malformed metadata; returns empty
 * so the caller falls back to the file's last-modified time.
 */
public final class MetadataReader {

    private static final Logger log = LoggerFactory.getLogger(MetadataReader.class);

    public Optional<Long> readCaptureDateMillis(Path file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());

            ExifSubIFDDirectory sub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (sub != null) {
                Date d = sub.getDateOriginal(TimeZone.getDefault());
                if (d == null) {
                    d = sub.getDateDigitized(TimeZone.getDefault());
                }
                if (d != null) {
                    return Optional.of(d.getTime());
                }
            }

            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                Date d = ifd0.getDate(ExifIFD0Directory.TAG_DATETIME, TimeZone.getDefault());
                if (d != null) {
                    return Optional.of(d.getTime());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.debug("No readable EXIF date for {}: {}", file, e.toString());
            return Optional.empty();
        }
    }
}
