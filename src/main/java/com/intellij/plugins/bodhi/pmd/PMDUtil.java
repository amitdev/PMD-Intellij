package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.containers.OrderedSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 * A Util class providing common functions for PMD plugin.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDUtil {

    public static final Pattern HOST_NAME_PATTERN = Pattern.compile(".+\\.([a-z]+\\.[a-z]+)/.+");
    public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final String JPINPOINT_RULES = "https://raw.githubusercontent.com/jborgers/PMD-jPinpoint-rules/master/rulesets/java/jpinpoint-rules.xml";
    public static final Map<String, String> KNOWN_CUSTOM_RULES = Map.of("jpinpoint-rules", JPINPOINT_RULES);

    /**
     * Not to be instantiated
     */
    private PMDUtil() {}

    /**
     * Get the the Project Component from given Action.
     * @param event AnAction event
     * @return the Project component related to the action
     */
    public static PMDProjectComponent getProjectComponent(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        return project.getComponent(PMDProjectComponent.class);
    }

    /**
     * Recursively find files of a criteria specified by given filter.
     * @param root The root directory or a file.
     * @param fileList The list where the results are added.
     */
    public static void listFiles(File root, List<File> fileList, FileFilter filter) {
        File[] files = root.listFiles(filter);
        if (files == null) {
            //root is a file
            fileList.add(root);
            return;
        }
        for (int x = 0; x < files.length; x++) {
            File file = files[x];
            if (file.isDirectory()) {
                listFiles(file, fileList, filter);
            } else {
                fileList.add(file);
            }
        }
    }

    public static void listFiles(VirtualFile item, final List<File> result, final VirtualFileFilter filter, final boolean skipDirectories) {
        VfsUtilCore.visitChildrenRecursively(item, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if(!filter.accept(file)) {
                    return false;
                }
                if(!file.isDirectory() || !skipDirectories) {
                    result.add(new File(file.getPresentableUrl()));
                }
                return true;
            }
        });
    }

    public static List<Module> getProjectModules(Project project) {
        return asList(ModuleManager.getInstance(project).getModules());
    }

    public static String getFullClassPathForAllModules(Project project) {
        List<Module> modules = getProjectModules(project);
        OrderedSet<String> uniqPaths = new OrderedSet<>();
        for (Module module : modules) {
            uniqPaths.addAll(OrderEnumerator.orderEntries(module).recursively().getPathsList().getPathList());
        }

        StringJoiner joiner = new StringJoiner(File.pathSeparator);
        for (String path : uniqPaths) {
            joiner.add(path);
        }
        return joiner.toString();
    }

    /**
     * Creates a java.io.FileFilter which filters files based on given extension.
     *
     * @param extension The extension of files to choose
     * @return the file filter
     */
    public static FileFilter createFileExtensionFilter(final String extension) {
        return new FileFilter() {
            public boolean accept(File pathname) {
                return isMatchingExtension(pathname, extension);
            }
        };
    }

    /**
     * Creates a javax.swing.filechooser.FileFilter which filters files
     * based on given extension.
     *
     * @param extension The extension of files to choose
     * @return the file filter
     */
    public static javax.swing.filechooser.FileFilter createFileExtensionFilter(final String extension, final String description) {
        return new javax.swing.filechooser.FileFilter() {
            public boolean accept(File pathname) {
                return isMatchingExtension(pathname, extension);
            }

            public String getDescription() {
                return description;
            }
        };
    }

    /**
     * Parses and returns the ruleset file name without extension from path.
     * By taking the filename from path and stripping off
     * the extension.
     *
     * @param rulePath the path
     * @return the rule name
     */
    public static String getBareFileNameFromPath(String rulePath) {
        String fileName = getFileNameFromPath(rulePath);
        int indexDot = fileName.indexOf('.');
        if (indexDot == -1) { // not found
            return fileName;
        }
        return fileName.substring(0, indexDot);
    }

    /**
     * Returns the file name including extension from path.
     *
     * @param rulePath the path
     * @return the rule file name including extension
     */
    public static String getFileNameFromPath(String rulePath) {
        int indexFilePath = rulePath.lastIndexOf(File.separatorChar);
        int indexUrl = rulePath.lastIndexOf('/'); // on windows different from previous
        int index = Math.max(indexFilePath, indexUrl); // fixes issue #147
        return rulePath.substring(index + 1); // if not found (-1), start from 0
    }

    /**
     * Parses and returns the extended rule file name from path, to include part of the path to distinguish between
     * the same ruleset in different locations. Include the file extension.
     *
     * @param rulePath the path of the rule set
     * @return the extended rule file name
     */
    public static String getExtendedFileNameFromPath(String rulePath) {
        int index = rulePath.lastIndexOf(File.separatorChar); // index of last '/'
        if (index == -1) {
            return rulePath;
        }
        String shortPathDesc;
        if (rulePath.startsWith("http")) {
            shortPathDesc = getHostNameFromPath(rulePath) + ": " + rulePath.substring(index + 1);
        }
        else {
            shortPathDesc = getFileBaseFromPath(rulePath) + ": " + rulePath.substring(index + 1);
        }
        return shortPathDesc;
    }

    /**
     * Returns the hostname like: githubusercontent.com from the path
     * @param rulePath
     * @return the hostname like: githubusercontent.com
     */
    private static String getHostNameFromPath(String rulePath) {
        String hostName = "";
        Matcher m = HOST_NAME_PATTERN.matcher(rulePath);
        if (m.matches()) {
            hostName = m.group(1);
        }
        return hostName;
    }

    /**
     * Returns the file base path like: /Users/john from the path
     * @param rulePath
     * @return the hostname like: githubusercontent.com
     */
    private static String getFileBaseFromPath(String rulePath) {
        int sepIndex1 = rulePath.indexOf(File.separatorChar);
        int sepIndex2 = rulePath.indexOf(File.separatorChar, sepIndex1 + 1);
        int sepIndex3 = rulePath.indexOf(File.separatorChar, sepIndex2 + 1);
        String fileBase = "";
        if (sepIndex3 > -1) {
            fileBase = rulePath.substring(0, sepIndex3);
        }
        return fileBase;
    }

    private static boolean isMatchingExtension(File pathname, String extension) {
        return pathname.isDirectory() || pathname.getName().endsWith("." + extension);
    }

    @NotNull
    public static String getRuleName(String ruleFileName) {
        int start = ruleFileName.lastIndexOf('/') + 1;
        int end = ruleFileName.indexOf('.');
        if (end == -1) end = ruleFileName.length();
        return ruleFileName.substring(start, end);
    }

    /**
     * Verify if url is non-empty and starts with http, specifies a host and is not malformed.
     * @param url the url to verify
     * @return whether url is non-empty and starts with http, specifies a host and is not malformed.
     */
    public static boolean isValidUrl(String url) {
        if (url == null || !url.startsWith("http")) {
            return false;
        }
        boolean isValid = true;
        try {
            URL myURL = new URL(url);
            String host = myURL.getHost();
            if (host == null || host.isEmpty()) {
                isValid = false;
            }
        } catch (MalformedURLException e) {
            isValid = false;
        }
        return isValid;
    }
}
