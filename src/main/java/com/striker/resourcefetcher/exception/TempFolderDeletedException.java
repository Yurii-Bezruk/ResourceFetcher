package com.striker.resourcefetcher.exception;

public class TempFolderDeletedException extends RuntimeException {
    public TempFolderDeletedException() {
        super("Temp folder has already been deleted!");
    }
}
