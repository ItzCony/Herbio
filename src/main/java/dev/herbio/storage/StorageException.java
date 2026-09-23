package dev.herbio.storage;

/** Failure of the configured storage backend, regardless of which one is active. */
public final class StorageException extends Exception {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
