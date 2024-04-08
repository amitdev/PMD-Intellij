package com.intellij.plugins.bodhi.pmd.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * AnAction which actions should be just accessing UI/Swing components, not model data.
 * EDT = Event Dispatch Thread
 * See ActionUpdateThread and ActionUpdateThreadAware
 * https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#action-implementation
 */
public abstract class AnEDTAction extends AnAction implements ActionUpdateThreadAware {

    // constructors from super class
    /**
     * Creates a new EDT action with its text, description and icon set to {@code null}.
     */
    public AnEDTAction() {
        super();
    }

    /**
     * Creates a new EDT action with the given {@code icon}, but without text or description.
     *
     * @param icon the default icon to appear in toolbars and menus. Note that some platforms don't have icons in the menu.
     */
    public AnEDTAction(@Nullable Icon icon) {
        super(icon);
    }

    /**
     * Creates a new EDT action with the given text, but without description or icon.
     *
     * @param text serves as a tooltip when the presentation is a button,
     *             and the name of the menu item when the presentation is a menu item (with mnemonic)
     */
    public AnEDTAction(@Nullable @NlsActions.ActionText String text) {
        super(text);
    }

    /**
     * Creates a new EDT action with the given text, but without description or icon.
     * Use this variant if you need to localize the action text.
     *
     * @param dynamicText serves as a tooltip when the presentation is a button,
     *                    and the name of the menu item when the presentation is a menu item (with mnemonic)
     */
    public AnEDTAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText) {
        super(dynamicText);
    }

    /**
     * Creates a new EDT action with the given text, description and icon.
     *
     * @param text        serves as a tooltip when the presentation is a button,
     *                    and the name of the menu item when the presentation is a menu item (with mnemonic)
     * @param description describes the current action,
     *                    this description will appear on the status bar when the presentation has the focus
     * @param icon        the action's icon
     */
    public AnEDTAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    public AnEDTAction(@NotNull @NlsActions.ActionText Supplier<String> text, @Nullable @NlsActions.ActionDescription Supplier<String> description, @Nullable Supplier<? extends @Nullable Icon> icon) {
        super(text, description, icon);
    }

    public AnEDTAction(@NotNull @NlsActions.ActionText Supplier<String> text, @NotNull @NlsActions.ActionDescription Supplier<String> description) {
        super(text, description);
    }

    /**
     * Creates a new EDT action with the given text, description and icon.
     * Use this variant if you need to localize the action text.
     *
     * @param dynamicText serves as a tooltip when the presentation is a button,
     *                    and the name of the menu item when the presentation is a menu item (with mnemonic)
     * @param icon        the action's icon
     */
    public AnEDTAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText, @Nullable Icon icon) {
        super(dynamicText, icon);
    }

    /**
     * Creates a new EDT action with the given text, description and icon.
     * Use this variant if you need to localize the action text or the description.
     *
     * @param dynamicText        serves as a tooltip when the presentation is a button,
     *                           and the name of the menu item when the presentation is a menu item (with mnemonic)
     * @param dynamicDescription describes the current action,
     *                           this description will appear on the status bar when the presentation has the focus
     * @param icon               the action's icon
     */
    public AnEDTAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText, @NotNull Supplier<@NlsActions.ActionDescription String> dynamicDescription, @Nullable Icon icon) {
        super(dynamicText, dynamicDescription, icon);
    }

    /**
     * Specifies to use the Event Dispatch Thread (EDT) and the EDT way {@link AnAction#update(AnActionEvent)},
     * {@link ActionGroup#getChildren(AnActionEvent)} or other update-like method shall be called.
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
