package com.atlassian.theplugin.idea.action.bamboo.changes;

import com.atlassian.theplugin.idea.ui.tree.file.BambooFileNode;
import com.atlassian.theplugin.util.CodeNavigationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * Copyright (C) 2008 Atlassian
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public abstract class AbstractBambooFileActions extends AnAction {
    @Nullable
    private TreePath getSelectedTreePath(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Component component = DataKeys.CONTEXT_COMPONENT.getData(dataContext);
        if (!(component instanceof JTree)) {
            return null;
        }
        final JTree theTree = (JTree) component;
        return theTree.getSelectionPath();
    }

    @Nullable
    protected BambooFileNode getBambooFileNode(AnActionEvent e) {
        TreePath treepath = getSelectedTreePath(e);
        if (treepath == null) {
            return null;
        }
        return getBambooFileNode(treepath);
    }

    protected void jumpToSource(final Project project, final BambooFileNode bfn) {
        String pathname = bfn.getBambooFileInfo().getFileDescriptor().getUrl();

        PsiFile[] psifiles = PsiManager.getInstance(project).getShortNamesCache().getFilesByName(bfn.getName());

        PsiFile matchingFile = CodeNavigationUtil.guessMatchingFile(pathname, psifiles, project.getBaseDir());
        if (matchingFile == null) {
            // TODO add file selection windo
        } else {
            matchingFile.navigate(true);
        }
    }

    @Nullable
    private BambooFileNode getBambooFileNode(TreePath path) {
        Object o = path.getLastPathComponent();
        if (o instanceof BambooFileNode) {
            return (BambooFileNode) o;
        }
        return null;

    }
}
