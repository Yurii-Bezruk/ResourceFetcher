package com.striker.resourcefetcher;

import com.striker.resourcefetcher.exception.ResourceException;
import com.striker.resourcefetcher.exception.TempFolderDeletedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * The TempFolder class represents temporary folder created for particular project.
 * Constructors of this class create new folder(-s) inside system's default temp folder.
 * After TempFolder has been created, it can be used to fetch resource file from jar file
 * using {@link #fetchResource(String)} or {@link #fetchResourceAsync(String)}
 * <p/>
 * TempFolder can be created using only project name: {@link #TempFolder(String)}, or
 * using group name and project name: {@link #TempFolder(String, String)}. When constructor
 * with group name used, TempFolder will create group folder first (if it does not yet exist),
 * and then project folder inside of it. This can be useful when building multiple applications
 * for the same organization, so all projects' temp folders will be located inside one group folder.
 * Group folder is never being deleted after it is created. When TempFolder object is deleted,
 * it only deletes project folder and its content.
 * <p/>
 * It is recommended to clear the folder with all resources after it has been used with {@link #delete()}
 * method. The class also implements {@link AutoCloseable} interface, which allows to use it with
 * try-with-resources block:
 * <blockquote><pre>
 *     try (TempFolder folder = new TempFolder(".test-group", "TestProject")) {
 *         ...
 *     }
 * </pre></blockquote>
 * This will ensure folder is deleted when execution exits try block
 */
public class TempFolder implements AutoCloseable {
    private static final String SYSTEM_TEMP_FOLDER = System.getProperty("java.io.tmpdir");
    private static final String SYSTEM_FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

    private final String groupFolderName;
    private final String projectFolderName;
    private final String folderPath;
    private final Map<String, Resource> fetchedResources;
    private boolean deleted;

    /**
     * Constructs new TempFolder instance with provided project name.
     * Calling this constructor automatically creates project folder in system's default
     * temp directory if it's missing.
     *
     * @param projectFolderName name for the project folder to be created
     * @throws ResourceException if unable to create directory
     */
    public TempFolder(String projectFolderName) throws ResourceException {
        this("", projectFolderName);
    }

    /**
     * Constructs new TempFolder instance with provided group name and project name.
     * Calling this constructor automatically creates group folder and
     * project folder in system's default temp directory if they're missing.
     * Project folder is being created inside of group folder.
     *
     * @param groupFolderName   name for the group folder to be created / accessed
     * @param projectFolderName name for the project folder to be created
     * @throws ResourceException if unable to create any of directories
     */
    public TempFolder(String groupFolderName, String projectFolderName) throws ResourceException {
        this.groupFolderName = groupFolderName;
        this.projectFolderName = projectFolderName;

        String tempFolder = SYSTEM_TEMP_FOLDER;
        if (isNotBlank(groupFolderName)) {
            tempFolder += groupFolderName + SYSTEM_FILE_SEPARATOR;
            createDirectory(tempFolder);
        }
        tempFolder += projectFolderName;
        createDirectory(tempFolder);
        this.folderPath = tempFolder;
        this.fetchedResources = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Fetches resource with given name into project temp folder and returns respective
     * {@link Resource} object. This method uses cache, so calling it second time for the same resource
     * will just provide reference to already created object without another file creation.
     * <p/>
     * Note that this method blocks the execution until file is fully extracted,
     * so it's not recommended to use it for large resource files. For asynchronous execution use
     * {@link #fetchResourceAsync(String)}
     *
     * @param resourceName name of the resource file
     * @return {@link Resource} object encapsulating fetched resource file in temp folder
     * @throws ResourceException          if unable to access resource or system temp folder
     * @throws TempFolderDeletedException if temp folder has already been deleted
     */
    public Resource fetchResource(String resourceName) throws ResourceException, TempFolderDeletedException {
        if (deleted) {
            throw new TempFolderDeletedException();
        }
        Resource resource = fetchedResources.getOrDefault(resourceName,
                new Resource(resourceName, folderPath + SYSTEM_FILE_SEPARATOR + resourceName, this));
        fetchedResources.put(resource.getName(), resource);
        return resource;
    }

    /**
     * Asynchronously fetches resource with given name into project temp folder and returns
     * {@link Future} of respective {@link Resource} object.
     * This method uses cache, so calling it second time for the same resource
     * will just provide reference to already created object without another file creation.
     * <p/>
     * For synchronous variant see {@link #fetchResource(String)}
     *
     * @param resourceName name of the resource file
     * @return {@link Future} of {@link Resource} object encapsulating fetched resource file in temp folder
     * @throws ResourceException          if unable to access resource or system temp folder
     * @throws TempFolderDeletedException if temp folder has already been deleted
     */
    public Future<Resource> fetchResourceAsync(String resourceName) throws ResourceException, TempFolderDeletedException {
        return CompletableFuture.supplyAsync(() -> fetchResource(resourceName));
    }

    /**
     * Deletes project temp folder and all files inside. Group folder remains untouched if exists.
     * After the folder has been deleted, it can no longer be used for resource fetching and doing so
     * will throw {@link TempFolderDeletedException}.
     *
     * @throws ResourceException if unable to delete folder or any file inside
     */
    public void delete() throws ResourceException {
        try (Stream<Path> walk = Files.walk(Paths.get(folderPath))) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            throw new ResourceException("Error deleting temp file: " + file, e);
                        }
                    });
        } catch (IOException e) {
            throw new ResourceException("Unable to access temp folder: " + folderPath, e);
        }
        deleted = true;
        fetchedResources.clear();
    }

    /**
     * The behaviour of this method is fully identical to {@link #delete()}. It is added to enable
     * usage of this object with try-with-resources block.
     *
     * @see #delete()
     */
    @Override
    public void close() {
        delete();
    }

    /**
     * Returns group folder name. If there were no group folder specified, returns empty string.
     *
     * @return group folder name or empty string
     */
    public String getGroupFolderName() {
        return groupFolderName;
    }

    /**
     * Returns project folder name.
     *
     * @return project folder name
     */
    public String getProjectFolderName() {
        return projectFolderName;
    }

    /**
     * Returns full system filepath to temp folder.
     * Return value of this method fully represents folder in the system.
     *
     * @return full filepath to temp folder
     */
    public String getFolderPath() {
        return folderPath;
    }

    /**
     * Returns {@link List} of all fetched {@link Resource} objects, that have not been deleted yet
     *
     * @return {@link List} of fetched {@link Resource} objects
     */
    public List<Resource> getFetchedResources() {
        return new ArrayList<>(fetchedResources.values());
    }

    void removeFetchedResource(String name) {
        fetchedResources.remove(name);
    }

    private void createDirectory(String path) throws ResourceException {
        File directory = new File(path);
        if (!directory.exists() && !directory.mkdir()) {
            throw new ResourceException("Failed to create directory " + path);
        }
    }
}
