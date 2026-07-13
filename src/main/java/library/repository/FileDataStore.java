package library.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import library.exception.DataStoreException;
import library.exception.ValidationException;

public final class FileDataStore implements DataStore {
    private static final String HEADER = "LIBRARY-DATA-V1";
    private static final Set<String> COLLECTIONS = Set.of("books", "members", "loans");
    private final Path directory;

    public FileDataStore(Path directory) {
        if (directory == null) throw new ValidationException("Storage directory must not be null.");
        this.directory = directory;
    }

    @Override
    public List<List<String>> read(String collectionName) {
        validateCollectionName(collectionName);
        Path path = pathFor(collectionName);
        if (!Files.exists(path)) return List.of();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
                throw new DataStoreException("Invalid data header in " + path.getFileName() + ".");
            }
            List<List<String>> records = new ArrayList<>();
            for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
                String line = lines.get(lineNumber);
                if (line.isEmpty()) throw new DataStoreException("Empty record at line " + (lineNumber + 1) + ".");
                String[] encodedFields = line.split("\\t", -1);
                List<String> fields = new ArrayList<>(encodedFields.length);
                for (String encoded : encodedFields) fields.add(decode(encoded, lineNumber + 1));
                records.add(List.copyOf(fields));
            }
            return List.copyOf(records);
        } catch (DataStoreException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new DataStoreException("Could not read " + path.getFileName() + ".", exception);
        }
    }

    @Override
    public void write(String collectionName, List<List<String>> records) {
        validateCollectionName(collectionName);
        if (records == null) throw new ValidationException("Records must not be null.");
        Path path = pathFor(collectionName);
        Path temporaryPath = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(directory);
            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            for (List<String> record : records) {
                if (record == null || record.isEmpty()) throw new ValidationException("Records must contain fields.");
                List<String> encoded = new ArrayList<>(record.size());
                for (String field : record) {
                    if (field == null) throw new ValidationException("Record fields must not be null.");
                    encoded.add(encode(field));
                }
                lines.add(String.join("\t", encoded));
            }
            Files.write(temporaryPath, String.join("\n", lines).concat("\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporaryPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (DataStoreException | ValidationException exception) {
            deleteTemporaryFile(temporaryPath);
            throw exception;
        } catch (IOException | RuntimeException exception) {
            deleteTemporaryFile(temporaryPath);
            throw new DataStoreException("Could not write " + path.getFileName() + ".", exception);
        }
    }

    private String decode(String encoded, int lineNumber) {
        if (!encoded.matches("[A-Za-z0-9_-]*")) {
            throw new DataStoreException("Invalid Base64 field at line " + lineNumber + ".");
        }
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(Base64.getUrlDecoder().decode(encoded)))
                    .toString();
            if (!encode(decoded).equals(encoded)) throw new DataStoreException("Invalid Base64 field at line " + lineNumber + ".");
            return decoded;
        } catch (IllegalArgumentException | CharacterCodingException exception) {
            throw new DataStoreException("Invalid Base64 field at line " + lineNumber + ".", exception);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Path pathFor(String collectionName) {
        return directory.resolve(collectionName + ".data");
    }

    private void validateCollectionName(String collectionName) {
        if (!COLLECTIONS.contains(collectionName)) throw new ValidationException("Unsupported collection name.");
    }

    private void deleteTemporaryFile(Path temporaryPath) {
        try { Files.deleteIfExists(temporaryPath); } catch (IOException ignored) { }
    }
}
