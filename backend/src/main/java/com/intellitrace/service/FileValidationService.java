package com.intellitrace.service;

import com.intellitrace.exception.FileValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Service providing validation checks for uploaded log files, verifying size constraints,
 * non-empty state, and permissible log file extensions.
 */
@Service
public class FileValidationService {

    // Default max size: 50 MB
    @Value("${intellitrace.upload.max-file-size:52428800}")
    private long maxFileSizeBytes;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".log", ".txt", ".out", ".err", ".trace", ".stdout", ".stderr"
    );

    /**
     * Validates an uploaded Spring {@link MultipartFile}.
     *
     * @param file the uploaded file
     * @throws FileValidationException if validation fails
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Uploaded log file is empty or missing");
        }

        String filename = file.getOriginalFilename();
        validateFilenameAndExtension(filename);
        validateSize(file.getSize());
    }

    /**
     * Validates raw file attributes directly.
     *
     * @param filename  name of the file
     * @param sizeBytes size of the file in bytes
     * @param isEmpty   flag indicating if file has no content
     */
    public void validate(String filename, long sizeBytes, boolean isEmpty) {
        if (isEmpty || sizeBytes <= 0) {
            throw new FileValidationException("Log file is empty or has zero bytes");
        }
        validateFilenameAndExtension(filename);
        validateSize(sizeBytes);
    }

    private void validateFilenameAndExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new FileValidationException("File name cannot be empty");
        }

        String lowerFilename = filename.toLowerCase();
        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
        if (!validExt) {
            throw new FileValidationException(
                    "Unsupported file format for '" + filename + "'. Permitted formats: " + ALLOWED_EXTENSIONS
            );
        }
    }

    private void validateSize(long sizeBytes) {
        if (sizeBytes > maxFileSizeBytes) {
            throw new FileValidationException(
                    "File size exceeds maximum allowed threshold of " + (maxFileSizeBytes / (1024 * 1024)) + " MB"
            );
        }
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
