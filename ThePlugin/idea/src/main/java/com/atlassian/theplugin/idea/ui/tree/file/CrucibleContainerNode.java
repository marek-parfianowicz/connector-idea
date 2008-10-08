package com.atlassian.theplugin.idea.ui.tree.file;

import com.atlassian.theplugin.commons.crucible.api.model.Review;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Icons;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;

public abstract class CrucibleContainerNode extends FileNode {
	private Review review;

	public CrucibleContainerNode(Review review) {
		super("container", null);
		this.review = review;
	}

	public TreeCellRenderer getTreeCellRenderer() {
		return new ColoredTreeCellRenderer() {
			public void customizeCellRenderer(
					JTree tree, Object value, boolean selected,
					boolean expanded, boolean leaf, int row, boolean hasFocus) {
				append(getText(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, null));
				setIcon(expanded ? Icons.DIRECTORY_OPEN_ICON : Icons.DIRECTORY_CLOSED_ICON);
			}
		};
	}

	protected abstract String getText();

	public Review getReview() {
		return review;
	}

	public void setReview(Review review) {
		this.review = review;
	}
}
