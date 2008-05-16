package com.atlassian.theplugin.crucible.api;

public interface CustomFilter {
    String getTitle();    

    String[] getState();

    String getAuthor();

    String getModerator();

    String getCreator();

    String getReviewer();

    boolean isComplete();

    boolean isAllReviewersComplete();

    String getProjectKey();

    boolean isOrRoles();
}
