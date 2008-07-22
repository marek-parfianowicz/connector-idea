/**
 * Copyright (C) 2008 Atlassian
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.theplugin.idea.ui.tree.comment;

import com.atlassian.theplugin.idea.ui.tree.AtlassianTreeNode;
import com.atlassian.theplugin.idea.ui.tree.AtlassianClickAction;
import com.atlassian.theplugin.commons.crucible.api.model.CrucibleFileInfo;
import com.atlassian.theplugin.util.CommentPanelBuilder;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.SimpleColoredComponent;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;

import javax.swing.tree.TreeCellRenderer;
import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: lguminski
 * Date: Jul 18, 2008
 * Time: 2:54:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileNameNode extends AtlassianTreeNode {
	private CrucibleFileInfo file;
	private static final TreeCellRenderer MY_RENDERER = new MyRenderer();

	public FileNameNode(CrucibleFileInfo file, AtlassianClickAction action) {
		super(action);
		this.file = file;
	}

	public CrucibleFileInfo getFile() {
		return file;
	}

	public TreeCellRenderer getTreeCellRenderer() {
		return MY_RENDERER;
	}

	private static class MyRenderer implements TreeCellRenderer {

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			JPanel panel = new JPanel(new FormLayout("4dlu, left:pref:grow, 4dlu", "4dlu, pref:grow, 4dlu"));
			SimpleColoredComponent component = new SimpleColoredComponent();
//			component.setFont(component.getFont().deriveFont(component.getFont().getSize() + 1));
			FileNameNode node = (FileNameNode) value;
			CrucibleFileInfo file = node.getFile();
			component.append(file.getFileDescriptor().getUrl(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

			StringBuilder txt = new StringBuilder();
			txt.append(" (rev: ");
			txt.append(file.getOldFileDescriptor().getRevision());
			txt.append("-");
			txt.append(file.getFileDescriptor().getRevision());
			txt.append(")");
			component.append(txt.toString(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
			panel.add(component, new CellConstraints(2, 2));
			

			panel.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder")
					: BorderFactory.createEmptyBorder(1, 1, 1, 1));
			return panel;

		}
	}
}
