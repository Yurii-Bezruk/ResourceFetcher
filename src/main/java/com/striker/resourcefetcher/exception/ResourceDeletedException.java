package com.striker.resourcefetcher.exception;

import com.striker.resourcefetcher.Resource;

/**
 * Thrown when attempt to access already deleted resource is detected
 */
public class ResourceDeletedException extends RuntimeException {
    /**
     * Constructs new ResourceDeletedException for particular {@link Resource} object.
     *
     * @param resource {@link Resource} that has already been deleted
     */
    public ResourceDeletedException(Resource resource) {
        super("Resource " + resource.getName() + " has already been deleted!");
    }
}
