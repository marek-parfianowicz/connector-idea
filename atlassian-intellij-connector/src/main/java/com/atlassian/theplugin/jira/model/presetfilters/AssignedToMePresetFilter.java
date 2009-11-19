package com.atlassian.theplugin.jira.model.presetfilters;

import com.atlassian.theplugin.jira.model.JiraPresetFilter;
import com.atlassian.theplugin.commons.jira.JiraServerData;

/**
 * User: kalamon
 * Date: 2009-11-18
 * Time: 14:42:34
 *
 * Runs query for: "assigneeSelect=issue_current_user&resolution=-1&sorter/field=priority"
 */
public class AssignedToMePresetFilter extends JiraPresetFilter {
    public AssignedToMePresetFilter(JiraServerData jiraServer) {
        super(jiraServer);
    }

    public String getName() {
        return "Assigned to me";
    }

    public String getQueryString() {
        return "assigneeSelect=issue_current_user&resolution=-1";
    }

    public String getSortBy() {
        return "priority";
    }
}