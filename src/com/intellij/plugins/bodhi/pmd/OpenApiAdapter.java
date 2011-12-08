package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.ide.TreeExpander;
import com.intellij.ide.CommonActionsManager;

import javax.swing.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * An Adapter class to provide backward compatibility to Intellij 5.0.
 * Some of the Open APIs are changed between 5.0 and 6.0. This class provides
 * a transparent mechanism for clients to invoke some of the APIs.
 *
 * @author bodhi
 * @version 1.0
 */
public class OpenApiAdapter {

    //Method to invoke
    private Method runProcessMethod;
    //Object to invoke above method
    private Object processRunner;

    private Method createCollapseMethod;
    private Method createExpandMethod;

    //Singletone instance
    private static final OpenApiAdapter instance = new OpenApiAdapter();

    /**
     * Prevents instantiation, as it is a singleton.
     */
    private OpenApiAdapter() {
        //Support both 5.x as well as 6.0
        String version = ApplicationInfo.getInstance().getMajorVersion();
        Class[] treeApiParamTypes = null;
        if (version.equals("5")) {
            //In 5.x, Application.runProcessWithProgressSynchronously() method is present
            processRunner = ApplicationManager.getApplication();
            //In 5.x, CommonActionsManager.createExpandAllAction() and
            // CommonActionsManager.createCollapseAllAction() takes one parameter
            treeApiParamTypes = new Class[] {TreeExpander.class};

        } else {
            //In 6.x onwards, runProcessWithProgressSynchronously is in ProgressManager
            processRunner = ProgressManager.getInstance();
            //In 6.x onwards, createCollapseAllAction and createExpandAllAction has 2 params
            treeApiParamTypes = new Class[] {TreeExpander.class, JComponent.class};
        }
        if (processRunner != null) {
            try {
                //Initialize the methods.
                Class[] paramTypes = new Class[] {Runnable.class, String.class, boolean.class, Project.class};
                runProcessMethod = processRunner.getClass().getMethod("runProcessWithProgressSynchronously", paramTypes);

                CommonActionsManager cam = CommonActionsManager.getInstance();
                createCollapseMethod = cam.getClass().getMethod("createCollapseAllAction", treeApiParamTypes);
                createExpandMethod = cam.getClass().getMethod("createExpandAllAction", treeApiParamTypes);

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the singleton instance.
     * @return the singleton instance.
     */
    public static OpenApiAdapter getInstance() {
        return instance;
    }

/**
 * Runs the specified operation in a background thread and shows a modal progress dialog in the
 * main thread while the operation is executing.
 *
 * @param params       the operation to execute (Runnable),
 *                     the title of the progress window,
 *                     whether "Cancel" button is shown on the progress window,
 *                     the project in the context of which the operation is executed.
 */
    public void runProcessWithProgressSynchronously(Object[] params) {
        if (runProcessMethod != null) {
            try {
                //Delegate to the correct api
                runProcessMethod.invoke(processRunner, params);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create the Collapse All action in toolbar.
     *
     * @param treeExpander The tree-expander to use
     * @param component The Component to use (6.0 and above)
     * @return the collapse all action
     */
    public AnAction createCollapseAllAction(TreeExpander treeExpander, JComponent component) {
        return runMethod(createCollapseMethod, treeExpander, component);
    }

    /**
     * Create the Expand All action in toolbar.
     *
     * @param treeExpander The tree-expander to use
     * @param component The Component to use (6.0 and above)
     * @return the expand all action
     */
    public AnAction createExpandAllAction(TreeExpander treeExpander, JComponent component) {
        return runMethod(createExpandMethod, treeExpander, component);
    }

    private AnAction runMethod(Method method, TreeExpander treeExpander, JComponent component) {
        Object[] params;
        //Provide parameters that are really required
        if (method.getParameterTypes().length > 1) {
            params = new Object[] { treeExpander, component };
        } else {
            params = new Object[] { treeExpander };
        }
        try {
            //Delegate to correct Api
            return (AnAction) method.invoke(CommonActionsManager.getInstance(), params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
