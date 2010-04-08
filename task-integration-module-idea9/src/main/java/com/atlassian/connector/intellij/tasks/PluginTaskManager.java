package com.atlassian.connector.intellij.tasks;

import com.atlassian.connector.cfg.ProjectCfgManager;
import com.atlassian.theplugin.commons.configuration.PluginConfiguration;
import com.atlassian.theplugin.commons.jira.JiraServerData;
import com.atlassian.theplugin.commons.remoteapi.ServerData;
import com.atlassian.theplugin.jira.model.ActiveJiraIssue;
import com.atlassian.theplugin.util.PluginUtil;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.TaskManagerImpl;
import com.intellij.tasks.jira.JiraRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;


/**
 * @author pmaruszak
 * @date Feb 2, 2010
 */
public class PluginTaskManager implements ProjectComponent {

    private final Project project;
    private final ProjectCfgManager projectCfgManager;
    private final PluginConfiguration pluginConfiguration;
    private TaskManagerImpl taskManager;
    private TaskListenerImpl listener;
    private Timer timer = new Timer("plugin task manager timer");
    private static final int SILENT_ACTIVATE_DELAY = 500;
//    private PluginChangeListAdapter changeListListener;


    public PluginTaskManager(Project project, ProjectCfgManager projectCfgManager, PluginConfiguration pluginConfiguration) {
        this.project = project;
        this.projectCfgManager = projectCfgManager;
        this.pluginConfiguration = pluginConfiguration;
        this.listener = new TaskListenerImpl(project, this, pluginConfiguration);
        this.taskManager = (TaskManagerImpl) TaskManager.getManager(project);
//        this.changeListListener = new PluginChangeListAdapter(project);
    }

    public void silentActivateIssue(final ActiveJiraIssue issue) {

        System.out.println("[" + new DateTime() + "]" + "[" + EventQueue.getCurrentEvent().getID() + "] silentActivateIssue : " + issue.getIssueUrl() + " thread : " + Thread.currentThread().getId());
        PluginUtil.getLogger().debug("silentActivating issue : " + issue.getIssueUrl());

//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {

        deactivateListner();
//            }
//        });


//            timer.schedule(new TimerTask() {
//
//                @Override
//                public void run() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    activateIssue(issue);
                } finally {
//                    activateListener();
                }
            }
        });

//                }
//            }, SILENT_ACTIVATE_DELAY);

       System.out.println("[" + new DateTime() + "]" + "[" + EventQueue.getCurrentEvent().getID() + "] [END] silentActivateIssue : " + issue.getIssueUrl() + " thread : " + Thread.currentThread().getId());
    }


    public void silentDeactivateIssue() {
        deactivateListner();
        try {
            deactivateToDefaultTask();
        } finally {

        }
    }

    public void activateIssue(final ActiveJiraIssue issue) {
        ServerData server = projectCfgManager.getServerr(issue.getServerId());
        final Task foundTask = findLocalTaskByUrl(issue.getIssueUrl());

        //ADD or GET JiraRepository
        BaseRepository jiraRepository = getJiraRepository(server);
        if (foundTask != null) {
            LocalTask activeTask = taskManager.getActiveTask();
            if ((activeTask.getIssueUrl() != null
                    && !activeTask.getIssueUrl().equals(foundTask.getIssueUrl()))) {
                try {

                    System.out.println("[" + new DateTime() + "]" + "[" + EventQueue.getCurrentEvent().getID() + "] activateIssue0+0 : " + issue.getIssueUrl() + " thread : " + Thread.currentThread().getId());
                    taskManager.activateTask(foundTask, true, false);


                    activateListener();

                } catch (Exception e) {
                    PluginUtil.getLogger().error("Task haven't been activated : " + e.getMessage());
                    deactivateToDefaultTask();
                }

            }
        } else {
            Task newTask = (Task) TaskHelper.findJiraTask((JiraRepository) jiraRepository, issue.getIssueKey());

            if (newTask != null) {
                try {
                    System.out.println("[" + new DateTime() + "]" + "[" + EventQueue.getCurrentEvent().getID() + "] activateIssue1 : " + issue.getIssueUrl() + " thread : " + Thread.currentThread().getId());
                    taskManager.activateTask(newTask, true, true);
                    activateListener();
//                    return (LocalTask) newTask;
                } catch (Exception e) {
                    PluginUtil.getLogger().error("Task haven't been activated : " + e.getMessage());
                    deactivateToDefaultTask();
                }
            }
        }
        System.out.println("[" + new DateTime() + "] [END] " + "[" + EventQueue.getCurrentEvent().getID() + "] activateIssue0+0 : " + issue.getIssueUrl() + " thread : " + Thread.currentThread().getId());
//        return getDefaultTask();
    }

    @Nullable
    private BaseRepository getJiraRepository(ServerData server) {
        TaskRepository[] repos = taskManager.getAllRepositories();
        if (repos != null) {
            for (TaskRepository r : repos) {
                if (r.getRepositoryType().getName().equalsIgnoreCase("JIRA")
                        && r.getUrl().equalsIgnoreCase(server.getUrl())) {
                    return (BaseRepository) r;
                }
            }
        }

        return createJiraRepository(server);
    }

    @Nullable
    private BaseRepository createJiraRepository(ServerData server) {
        BaseRepository repo = (BaseRepository) TaskHelper.createJiraRepository();
        repo.setPassword(server.getPassword());
        repo.setUrl(server.getUrl());
        repo.setUsername(server.getUsername());
        addJiraRepository(repo);

        return null;
    }

    public void activateListener() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PluginUtil.getLogger().debug("Activating TM listener");
                String id = EventQueue.isDispatchThread() && EventQueue.getCurrentEvent() != null ? String.valueOf(EventQueue.getCurrentEvent().getID()) : " null";
                System.out.println("[" + new DateTime() + "]" + "[" + id + "] activateListener" + " thread : " + Thread.currentThread().getId());
                taskManager.addTaskListener(listener);
                System.out.println("[" + new DateTime() + "] [END]" + "[" + id + "] activateListener" + " thread : " + Thread.currentThread().getId());

            }
        });

    }

    public void deactivateListner() {
        PluginUtil.getLogger().debug("Deactivating TM listener");
        System.out.println("[" + new DateTime() + "]" + "[" + EventQueue.getCurrentEvent().getID() + "] deactivateListner" + " thread : " + Thread.currentThread().getId());
        taskManager.removeTaskListener(listener);
        System.out.println("[" + new DateTime() + "] [END]" + "[" + EventQueue.getCurrentEvent().getID() + "] deactivateListner" + " thread : " + Thread.currentThread().getId());
    }

    private void addJiraRepository(TaskRepository repo) {
        TaskRepository[] repos = taskManager.getAllRepositories();
        List<TaskRepository> reposList = new ArrayList<TaskRepository>();
        if (repos != null) {
            for (TaskRepository r : repos) {
                reposList.add(r);
            }
        }
        reposList.add(repo);
        taskManager.setRepositories(reposList);
    }

    @Nullable
    private LocalTask findLocalTaskByUrl
            (String
                    issueUrl) {
        LocalTask[] tasks = taskManager.getLocalTasks();
        if (tasks != null) {
            for (LocalTask t : tasks) {
                if (t.getIssueUrl() != null && t.getIssueUrl().equals(issueUrl)) {
                    return t;
                }
            }
        }

        return null;
    }

    @Nullable
    private LocalTask findLocalTaskById
            (String
                    issueId) {
        LocalTask[] tasks = taskManager.getLocalTasks();
        if (tasks != null) {
            for (LocalTask t : tasks) {
                if (t.getId() != null && t.getId().equals(issueId)) {
                    return t;
                }
            }
        }

        return null;
    }

    public void projectOpened() {
        StartupManager.getInstance(project).registerPostStartupActivity(new Runnable() {
            public void run() {
                initializePlugin();
            }
        });
    }


    private void initializePlugin() {
        this.taskManager = (TaskManagerImpl) TaskManager.getManager(project);
        activateListener();
//        ChangeListManagerImpl.getInstance(project).addChangeListListener(changeListListener);
    }

    public void projectClosed() {
        deactivateListner();
    }

    @NotNull
    public String getComponentName() {
        return "PluginTaskManager";
    }

    public void initComponent() {

    }

    public void disposeComponent() {

    }

    public static boolean isDefaultTask(LocalTask task) {
        return (task.getId() != null && task.getId().equalsIgnoreCase("Default")) || task.getSummary().equalsIgnoreCase("Default task");
    }

    @Nullable
    JiraServerData findJiraPluginJiraServer(String issueUrl) {
        for (JiraServerData server : projectCfgManager.getAllEnabledJiraServerss()) {
            if (issueUrl != null && issueUrl.contains(server.getUrl())) {
                return server;
            }
        }

        return null;
    }


    public void deactivateToDefaultTask() {
        PluginUtil.getLogger().debug("deactivating to default");
        System.out.println("[" + new DateTime() + "]" + "[" + EventQueue.getCurrentEvent().getID() + "] activateListener" + " thread : " + Thread.currentThread().getId());
        LocalTask defaultTask = getDefaultTask();
        if (defaultTask != null) {
            taskManager.activateTask(defaultTask, false, false);
        }
        activateListener();
    }

    private LocalTask getDefaultTask() {
        LocalTask defaultTask = findLocalTaskById("Default task");
        if (defaultTask == null) {
            defaultTask = findLocalTaskById("Default");
        }

        return defaultTask;
    }

}
