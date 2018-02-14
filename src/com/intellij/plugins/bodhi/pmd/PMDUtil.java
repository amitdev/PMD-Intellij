package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * A Util class providing common functions for PMD plugin.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDUtil {

    /**
     * Get the the Project Component from given Action.
     * @param event AnAction event
     * @return the Project component related to the action
     */
    public static PMDProjectComponent getProjectComponent(AnActionEvent event) {
        Project project = event.getData(DataKeys.PROJECT);
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
     * Parses and returns the rule name from path.
     * Rulename is got by getting the filename from path and stripping off
     * the extension.
     *
     * @param rulePath the path
     * @return the rule name
     */
    public static String getRuleNameFromPath(String rulePath) {
        int index = rulePath.lastIndexOf(File.separatorChar);
        int indexDot = rulePath.indexOf('.', index);
        if (indexDot == -1) {
            indexDot = rulePath.length();
        }
        String ruleName = rulePath;
        if (index != -1) {
            ruleName = rulePath.substring(index+1, indexDot);
        }
        return ruleName;
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
}
