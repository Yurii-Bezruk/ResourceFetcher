package com.striker.resourcefetcher;

import com.striker.resourcefetcher.exception.ResourceDeletedException;
import com.striker.resourcefetcher.exception.ResourceException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The Resource class represents fetched resource file in system temp folder.
 * By the time this object is created, the file has already been created in folder.
 * The object of the class should not be created on its own, and can be obtained only using
 * {@link TempFolder#fetchResource(String)} or {@link TempFolder#fetchResourceAsync(String)}
 *
 * @see TempFolder
 */
public class Resource {
    private final String name;
    private final String path;
    private final TempFolder tempFolder;
    private boolean deleted;

    Resource(String name, String destinationPath, TempFolder tempFolder) throws ResourceException {
        this.name = name;
        this.path = destinationPath;
        this.tempFolder = tempFolder;

        try (InputStream resource = getClass().getResourceAsStream("/" + this.name)) {
            if (resource == null) {
                throw new ResourceException("Unable to access resource '" + this.name + "'");
            }
            try (FileOutputStream destinationFile = new FileOutputStream(this.path)) {
                destinationFile.write(resource.readAllBytes());
            } catch (IOException e) {
                throw new ResourceException("Unable to access destination path '" + destinationPath + "'", e);
            }
        } catch (IOException e) {
            throw new ResourceException("Unable to access resource '" + this.name + "'", e);
        }
    }

    /**
     * Returns file name of this resource in resources folder
     *
     * @return resource name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns full system filepath to the fetched resource file in temp folder, including
     * resource name. Return value of this method fully represents file in the system.
     *
     * @return full filepath to fetched resource
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns {@link TempFolder} that contains this resource
     *
     * @return {@link TempFolder} associated with resource
     */
    public TempFolder getTempFolder() {
        return tempFolder;
    }

    /**
     * Indicates whether this resource has already been deleted from temp folder
     *
     * @return true if the resource has been deleted, false otherwise
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Returns {@link InputStream} of resource file content. The stream is obtained using
     * {@link FileInputStream} opened in {@link #getPath()}. The InputStream represents fetched file,
     * located in temp folder and not the original resource archived in jar-file.
     * To obtain the original resource from jar use {@link Class#getResourceAsStream(String)}
     *
     * @return full filepath to fetched resource
     * @throws ResourceException        if unable to construct the stream
     * @throws ResourceDeletedException if resource has already been deleted
     * @see Class
     */
    public InputStream getInputStream() throws ResourceException, ResourceDeletedException {
        if (deleted) {
            throw new ResourceDeletedException(this);
        }
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new ResourceException("Unable to create input stream from file '" + this.path + "'", e);
        }
    }

    /**
     * Deletes this resource from temp folder. All other resources from temp folder remain untouched.
     * This action does not affect original archived resource in jar file. This method should only be
     * used when there's a string need to delete only single resource, while continuing to work with others.
     * To delete all resources in temp folder use {@link TempFolder#delete()}
     *
     * @throws ResourceException if unable to delete file
     */
    public void delete() throws ResourceException {
        try {
            Files.delete(Paths.get(path));
        } catch (IOException e) {
            throw new ResourceException("Unable to delete file '" + this.path + "'", e);
        }
        tempFolder.removeFetchedResource(name);
        deleted = true;
    }
}
