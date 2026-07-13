package library.exception;

public final class DataStoreException extends LibraryException {
    public DataStoreException(String message) { super(message); }
    public DataStoreException(String message, Throwable cause) { super(message, cause); }
}
