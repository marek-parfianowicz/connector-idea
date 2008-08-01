package com.atlassian.theplugin.idea.crucible.events;

import com.atlassian.theplugin.idea.crucible.comments.CrucibleReviewActionListener;
import com.atlassian.theplugin.idea.crucible.ReviewData;
import com.atlassian.theplugin.commons.crucible.api.model.CrucibleFileInfo;
import com.atlassian.theplugin.commons.crucible.api.model.VersionedCommentBean;
import com.intellij.openapi.editor.Editor;

/**
 * Created by IntelliJ IDEA.
 * User: lguminski
 * Date: Jul 28, 2008
 * Time: 7:33:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class VersionedCommentAboutToAdd extends CrucibleEvent {
	private ReviewData review;
	private CrucibleFileInfo file;
	private VersionedCommentBean newComment;
	private Editor editor;

	public VersionedCommentAboutToAdd(CrucibleReviewActionListener caller, ReviewData review,
									  CrucibleFileInfo file, VersionedCommentBean newComment, Editor editor) {
		super(caller);
		this.review = review;
		this.file = file;
		this.newComment = newComment;
		this.editor = editor;
	}

	protected void notify(CrucibleReviewActionListener listener) {
		listener.aboutToAddVersionedComment(review, file, newComment, editor);
	}
}
