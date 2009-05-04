package com.atlassian.theplugin.idea.jira;

import com.atlassian.theplugin.cache.RecentlyOpenIssuesCache;
import com.atlassian.theplugin.cfg.CfgUtil;
import com.atlassian.theplugin.commons.ServerType;
import com.atlassian.theplugin.commons.cfg.*;
import com.atlassian.theplugin.commons.configuration.PluginConfiguration;
import com.atlassian.theplugin.commons.remoteapi.RemoteApiException;
import com.atlassian.theplugin.commons.remoteapi.ServerData;
import com.atlassian.theplugin.configuration.IssueRecentlyOpenBean;
import com.atlassian.theplugin.configuration.JiraFilterConfigurationBean;
import com.atlassian.theplugin.configuration.JiraWorkspaceConfiguration;
import com.atlassian.theplugin.idea.Constants;
import com.atlassian.theplugin.idea.IdeaHelper;
import com.atlassian.theplugin.idea.PluginToolWindowPanel;
import com.atlassian.theplugin.idea.action.issues.RunIssueActionAction;
import com.atlassian.theplugin.idea.action.issues.activetoolbar.ActiveIssueUtils;
import com.atlassian.theplugin.idea.config.IntelliJProjectCfgManager;
import com.atlassian.theplugin.idea.jira.tree.JIRAFilterTree;
import com.atlassian.theplugin.idea.jira.tree.JIRAIssueTreeBuilder;
import com.atlassian.theplugin.idea.jira.tree.JiraFilterTreeSelectionListener;
import com.atlassian.theplugin.idea.ui.DialogWithDetails;
import com.atlassian.theplugin.idea.ui.PopupAwareMouseAdapter;
import com.atlassian.theplugin.jira.JIRAIssueProgressTimestampCache;
import com.atlassian.theplugin.jira.JIRAServerFacade;
import com.atlassian.theplugin.jira.JIRAServerFacadeImpl;
import com.atlassian.theplugin.jira.api.*;
import com.atlassian.theplugin.jira.model.*;
import com.atlassian.theplugin.remoteapi.MissingPasswordHandlerJIRA;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.TreeSpeedSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public final class IssuesToolWindowPanel extends PluginToolWindowPanel implements DataProvider, IssueActionProvider {

	public static final String PLACE_PREFIX = IssuesToolWindowPanel.class.getSimpleName();
	private IntelliJProjectCfgManager projectCfgManager;
	private final CfgManager cfgManager;
	private final PluginConfiguration pluginConfiguration;
	private JiraWorkspaceConfiguration jiraWorkspaceConfiguration;

	private static final String SERVERS_TOOL_BAR = "ThePlugin.JiraServers.ServersToolBar";
	private JIRAFilterListModel jiraFilterListModel;
	private JIRAIssueTreeBuilder issueTreeBuilder;
	private JIRAIssueListModelBuilder jiraIssueListModelBuilder;
	private RecentlyOpenIssuesCache recentlyOpenIssuesCache;
	private JIRAFilterListBuilder jiraFilterListModelBuilder;
	private JiraIssueGroupBy groupBy;
	@NotNull
	private final JiraManualFilterDetailsPanel manualFilterEditDetailsPanel;
	private JIRAFilterTree jiraFilterTree;

	private JIRAServerFacade jiraServerFacade;

	private JIRAIssueListModel currentIssueListModel;

	private SearchingJIRAIssueListModel searchingIssueListModel;

	private JIRAServerModel jiraServerModel;

	private ConfigurationListener configListener = new LocalConfigurationListener();

	private boolean groupSubtasksUnderParent;

	private static final String THE_PLUGIN_JIRA_ISSUES_ISSUES_TOOL_BAR = "ThePlugin.JiraIssues.IssuesToolBar";

	private JIRAIssueListModel baseIssueListModel;

	private Timer timer;

	private static final int ONE_SECOND = 1000;

	public IssuesToolWindowPanel(@NotNull final Project project,
			@NotNull final IntelliJProjectCfgManager projectCfgManager,
			@NotNull final CfgManager cfgManager,
			@NotNull final PluginConfiguration pluginConfiguration,
			@NotNull final JiraWorkspaceConfiguration jiraWorkspaceConfiguration,
			@NotNull final IssueToolWindowFreezeSynchronizator freezeSynchronizator,
			@NotNull final JIRAIssueListModel issueModel,
			@NotNull final JIRAIssueListModelBuilder jiraIssueListModelBuilder,
			@NotNull final RecentlyOpenIssuesCache recentlyOpenIssuesCache,
			@NotNull final JIRAFilterListBuilder filterListBuilder,
			@NotNull final JIRAServerModel jiraServerModel) {
		super(project, SERVERS_TOOL_BAR, THE_PLUGIN_JIRA_ISSUES_ISSUES_TOOL_BAR);

		this.projectCfgManager = projectCfgManager;
		this.cfgManager = cfgManager;
		this.pluginConfiguration = pluginConfiguration;
		this.jiraWorkspaceConfiguration = jiraWorkspaceConfiguration;
		this.jiraIssueListModelBuilder = jiraIssueListModelBuilder;
		this.recentlyOpenIssuesCache = recentlyOpenIssuesCache;

		jiraServerFacade = JIRAServerFacadeImpl.getInstance();

		if (jiraWorkspaceConfiguration.getView() != null && jiraWorkspaceConfiguration.getView().getGroupBy() != null) {
			groupBy = jiraWorkspaceConfiguration.getView().getGroupBy();
			groupSubtasksUnderParent = jiraWorkspaceConfiguration.getView().isCollapseSubtasksUnderParent();
		} else {
			groupBy = JiraIssueGroupBy.TYPE;
			groupSubtasksUnderParent = false;
		}
		jiraFilterListModel = getJIRAFilterListModel();
		baseIssueListModel = issueModel;
		JIRAIssueListModel sortingIssueListModel = new SortingByPriorityJIRAIssueListModel(baseIssueListModel);
		searchingIssueListModel = new SearchingJIRAIssueListModel(sortingIssueListModel);
		currentIssueListModel = searchingIssueListModel;

		issueTreeBuilder = new JIRAIssueTreeBuilder(getGroupBy(), groupSubtasksUnderParent, currentIssueListModel, project,
				projectCfgManager);

		this.jiraServerModel = jiraServerModel;

		jiraIssueListModelBuilder.setModel(baseIssueListModel);
		jiraFilterListModelBuilder = filterListBuilder;
		if (jiraFilterListModelBuilder != null) {
			jiraFilterListModelBuilder.setListModel(jiraFilterListModel);
			jiraFilterListModelBuilder.setProjectId(CfgUtil.getProjectId(project));
			jiraFilterListModelBuilder.setJiraWorkspaceCfg(jiraWorkspaceConfiguration);
		}
		currentIssueListModel.addModelListener(new LocalJiraIssueListModelListener());

		currentIssueListModel.addFrozenModelListener(new FrozenModelListener() {
			public void modelFrozen(FrozenModel model, boolean frozen) {
				if (getStatusBarPane() != null) {
					getStatusBarPane().setEnabled(!frozen);
				}
				if (getSearchField() != null) {
					getSearchField().setEnabled(!frozen);
				}
			}
		});

		manualFilterEditDetailsPanel = new JiraManualFilterDetailsPanel(jiraFilterListModel, this.jiraWorkspaceConfiguration,
				getProject(), jiraServerModel);

		getStatusBarPane().addMoreIssuesListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				refreshIssues(false);
			}
		});

		addIssuesTreeListeners();
		addSearchBoxListener();
		freezeSynchronizator.setIssueModel(currentIssueListModel);
		freezeSynchronizator.setServerModel(jiraServerModel);
		freezeSynchronizator.setFilterModel(jiraFilterListModel);

		init(0);


		jiraFilterTree.addSelectionListener(new LocalJiraFilterTreeSelectionListener());

		jiraFilterListModel.addModelListener(new JIRAFilterListModelListener() {
			public void manualFilterChanged(final JIRAManualFilter manualFilter, final ServerData jiraServer) {
				// refresh issue list
				refreshIssues(manualFilter, jiraServer, true);
			}

			public void modelChanged(final JIRAFilterListModel listModel) {
			}

			public void serverRemoved(final JIRAFilterListModel filterListModel) {
			}

			public void serverAdded(final JIRAFilterListModel filterListModel) {
			}

			public void serverNameChanged(final JIRAFilterListModel filterListModel) {
			}
		});

	}

	protected void showManualFilterPanel(final JIRAManualFilter manualFilter, final ServerData jiraServerCfg) {
		getSplitLeftPane().setOrientation(true);
		manualFilterEditDetailsPanel.setFilter(manualFilter, jiraServerCfg);
		getSplitLeftPane().setSecondComponent(manualFilterEditDetailsPanel);
		getSplitLeftPane().setProportion(MANUAL_FILTER_PROPORTION_VISIBLE);
	}

	protected void hideManualFilterPanel() {
		getSplitLeftPane().setOrientation(true);
		getSplitLeftPane().setSecondComponent(null);
		getSplitLeftPane().setProportion(MANUAL_FILTER_PROPORTION_HIDDEN);
	}

	public IntelliJProjectCfgManager getProjectCfgManager() {
		return projectCfgManager;
	}

	public void init() {
		recentlyOpenIssuesCache.init();
	}

	@Override
	protected void addSearchBoxListener() {
		getSearchField().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				triggerDelayedSearchBoxUpdate();
			}

			public void removeUpdate(DocumentEvent e) {
				triggerDelayedSearchBoxUpdate();
			}

			public void changedUpdate(DocumentEvent e) {
				triggerDelayedSearchBoxUpdate();
			}
		});
		getSearchField().addKeyboardListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					getSearchField().addCurrentTextToHistory();
				}
			}

			public void keyReleased(KeyEvent e) {
			}
		});
	}

	private void triggerDelayedSearchBoxUpdate() {
		if (timer != null && timer.isRunning()) {
			return;
		}
		timer = new Timer(ONE_SECOND, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchingIssueListModel.setSearchTerm(getSearchField().getText());
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	public JIRAIssueListModel getBaseIssueListModel() {
		return baseIssueListModel;
	}


	private void addIssuesTreeListeners() {
		getRightTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				JIRAIssue issue = getSelectedIssue();
				if (e.getKeyCode() == KeyEvent.VK_ENTER && issue != null) {
					openIssueWithSelectedServer(issue);
				}
			}
		});

		getRightTree().addMouseListener(new PopupAwareMouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				final JIRAIssue issue = getSelectedIssue();
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && issue != null) {
					openIssueWithSelectedServer(issue);
				}
			}

			@Override
			protected void onPopup(MouseEvent e) {
				int selRow = getRightTree().getRowForLocation(e.getX(), e.getY());
				TreePath selPath = getRightTree().getPathForLocation(e.getX(), e.getY());
				if (selRow != -1 && selPath != null) {
					getRightTree().setSelectionPath(selPath);
					if (getSelectedIssue() != null) {
						launchContextMenu(e);
					}
				}
			}
		});
	}

	private void launchContextMenu(MouseEvent e) {
		final DefaultActionGroup actionGroup = new DefaultActionGroup();

		final ActionGroup configActionGroup = (ActionGroup) ActionManager
				.getInstance().getAction("ThePlugin.JiraIssues.IssuePopupMenu");
		actionGroup.addAll(configActionGroup);

		final ActionPopupMenu popup = ActionManager.getInstance().createActionPopupMenu("Context menu", actionGroup);
		addIssueActionsSubmenu(actionGroup, popup);

		final JPopupMenu jPopupMenu = popup.getComponent();
		jPopupMenu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void addIssueActionsSubmenu(DefaultActionGroup actionGroup, final ActionPopupMenu popup) {
		final DefaultActionGroup submenu = new DefaultActionGroup("Querying for Actions... ", true) {
			@Override
			public void update(AnActionEvent event) {
				super.update(event);

				if (getChildrenCount() > 0) {
					event.getPresentation().setText("Issue Workflow Actions");
				}
			}
		};
		actionGroup.add(submenu);

		final JIRAIssue issue = getSelectedIssue();
		List<JIRAAction> actions = JiraIssueAdapter.get(issue).getCachedActions();
		if (actions != null) {
			for (JIRAAction a : actions) {
				submenu.add(new RunIssueActionAction(this, jiraServerFacade, issue, a, jiraIssueListModelBuilder));
			}
		} else {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						ServerData jiraServer = issue.getServer(); // was: getSelectedServer();

						if (jiraServer != null) {
							final List<JIRAAction> actions = jiraServerFacade.getAvailableActions(jiraServer, issue);

							JiraIssueAdapter.get(issue).setCachedActions(actions);
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									JPopupMenu pMenu = popup.getComponent();
									if (pMenu.isVisible()) {
										for (JIRAAction a : actions) {
											submenu.add(new RunIssueActionAction(IssuesToolWindowPanel.this,
													jiraServerFacade, issue, a, jiraIssueListModelBuilder));
										}

										// magic that makes the popup update itself. Don't ask - it is some sort of voodoo
										pMenu.setVisible(false);
										pMenu.setVisible(true);
									}
								}
							});
						}
					} catch (JIRAException e) {
						setStatusMessage("Query for issue " + issue.getKey() + " actions failed: " + e.getMessage(), true);
					} catch (NullPointerException e) {
						// somebody unselected issue in the table, so let's just skip
					}
				}
			};
			t.start();
		}
	}

	private JIRAIssue getSelectedIssue() {
		return getRightTree().getSelectedIssue();
	}

	public void openIssue(@NotNull JIRAIssue issue) {
		if (issue.getServer() != null) {
			recentlyOpenIssuesCache.addIssue(issue);
			ActiveIssueUtils.checkIssueState(project, issue);
			IdeaHelper.getIssueToolWindow(getProject()).showIssue(issue, baseIssueListModel);

		}
	}

	public void openIssueWithSelectedServer(@NotNull JIRAIssue issue) {
		if (getSelectedServer() != null) {
			openIssue(issue);
		} else if (isRecentlyOpenFilterSelected()) {
			for (JiraServerCfg server
					: projectCfgManager.getCfgManager().getAllEnabledJiraServers(CfgUtil.getProjectId(project))) {
				if (server.getUrl().equals(issue.getServerUrl())) {
					openIssue(issue);
					break;
				}
			}
		}
	}

	public void openIssue(@NotNull final String issueKey, @NotNull final ServerData jiraServer) {
		JIRAIssue issue = null;
		for (JIRAIssue i : baseIssueListModel.getIssues()) {
			if (i.getKey().equals(issueKey) && i.getServer().getServerId().equals(jiraServer.getServerId())) {
				issue = i;
				break;
			}
		}

		if (issue != null) {
			openIssue(issue);
		} else {
			Task.Backgroundable task = new Task.Backgroundable(getProject(), "Fetching JIRA issue " + issueKey, false) {
				private JIRAIssue issue;
				private Throwable exception;

				@Override
				public void onSuccess() {
					if (getProject().isDisposed()) {
						return;
					}
					if (exception != null) {
						final String serverName = jiraServer != null ? jiraServer.getName() : "[UNDEFINED!]";
						DialogWithDetails.showExceptionDialog(getProject(),
								"Cannot fetch issue " + issueKey + " from server " + serverName, exception);
						return;
					}
					if (issue != null) {
						openIssue(issue);
					}
				}

				@Override
				public void run(@NotNull ProgressIndicator progressIndicator) {
					progressIndicator.setIndeterminate(true);
					try {
						if (jiraServer != null) {
							issue = jiraServerFacade.getIssue(jiraServer, issueKey);
							jiraIssueListModelBuilder.updateIssue(issue);
						} else {
							exception = new RuntimeException("No JIRA server defined!");
						}
					} catch (JIRAException e) {
						exception = e;
					}
				}
			};
			task.queue();
		}
	}

	public void assignIssueToMyself(@NotNull final JIRAIssue issue) {
		if (issue == null) {
			return;
		}
		try {
			ServerData jiraServer = getSelectedServer();
			if (jiraServer != null) {
				assignIssue(issue, jiraServer.getUserName());
			}
		} catch (NullPointerException ex) {
			// whatever, means action was called when no issue was selected. Let's just swallow it
		}
	}

	public void assignIssueToSomebody(@NotNull final JIRAIssue issue) {
		if (issue == null) {
			return;
		}
		final GetUserNameDialog getUserNameDialog = new GetUserNameDialog(issue.getKey());
		getUserNameDialog.show();
		if (getUserNameDialog.isOK()) {
			try {
				assignIssue(issue, getUserNameDialog.getName());
			} catch (NullPointerException ex) {
				// whatever, means action was called when no issue was selected. Let's just swallow it
			}
		}
	}

	private void assignIssue(final JIRAIssue issue, final String assignee) {

		Task.Backgroundable assign = new Task.Backgroundable(getProject(), "Assigning Issue", false) {

			@Override
			public void run(@NotNull final ProgressIndicator indicator) {
				setStatusMessage("Assigning issue " + issue.getKey() + " to " + assignee + "...");
				try {

					ServerData jiraServer = getSelectedServer();
					if (jiraServer != null) {
						jiraServerFacade.setAssignee(jiraServer, issue, assignee);
						setStatusMessage("Assigned issue " + issue.getKey() + " to " + assignee);
						jiraIssueListModelBuilder.reloadIssue(issue.getKey(), jiraServer);
					}
				} catch (JIRAException e) {
					setStatusMessage("Failed to assign issue " + issue.getKey() + ": " + e.getMessage(), true);
				}
			}
		};

		ProgressManager.getInstance().run(assign);
	}

	public boolean createChangeListAction(@NotNull final JIRAIssue issue) {
		String changeListName = issue.getKey() + " - " + issue.getSummary();
		final ChangeListManager changeListManager = ChangeListManager.getInstance(getProject());
		ChangesetCreate c;

		LocalChangeList changeList = changeListManager.findChangeList(changeListName);
		if (changeList == null) {
			c = new ChangesetCreate(issue.getKey());
			c.setChangesetName(changeListName);
			c.setChangestComment(changeListName + "\n");
			c.setActive(true);
			c.show();
			if (c.isOK()) {
				changeListName = c.getChangesetName();
				changeList = changeListManager.addChangeList(changeListName, c.getChangesetComment());
				if (c.isActive()) {
					changeListManager.setDefaultChangeList(changeList);
				}
			}
		} else {
			changeListManager.setDefaultChangeList(changeList);
			return true;
		}

		return c.isOK();
	}

	public void addCommentToSelectedIssue() {
		// todo move getSelectedIssue from the model to the tree
		final JIRAIssue issue = getSelectedIssue();
		if (issue != null) {
			addCommentToIssue(issue.getKey(), issue.getServer());
		}
	}

	public void addCommentToIssue(final String issueKey, final ServerData jiraServer) {
		final IssueCommentDialog issueCommentDialog = new IssueCommentDialog(issueKey);
		issueCommentDialog.show();
		if (issueCommentDialog.isOK()) {
			Task.Backgroundable comment = new Task.Backgroundable(getProject(), "Commenting Issue", false) {
				@Override
				public void run(@NotNull final ProgressIndicator indicator) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							setStatusMessage("Commenting issue " + issueKey + "...");
						}
					});
					try {
						if (jiraServer != null) {
							jiraServerFacade.addComment(jiraServer, issueKey, issueCommentDialog.getComment());
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									setStatusMessage("Commented issue " + issueKey);
								}
							});
						}
					} catch (final JIRAException e) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								setStatusMessage("Issue not commented: " + e.getMessage(), true);
							}
						});
					}
				}
			};

			ProgressManager.getInstance().run(comment);
		}
	}

	public boolean logWorkOrDeactivateIssue(final JIRAIssue issue, final ServerData jiraServer, String initialLog,
			final boolean deactivateIssue) {
		if (issue != null) {
			final WorkLogCreateAndMaybeDeactivateDialog dialog =
					new WorkLogCreateAndMaybeDeactivateDialog(jiraServer, issue, getProject(), initialLog, deactivateIssue);
			dialog.show();
			if (dialog.isOK()) {
				Task.Backgroundable logWork = new LogWorkWorkerTask(issue, dialog, jiraServer, deactivateIssue);
				ProgressManager.getInstance().run(logWork);
			}

			return dialog.isOK();
		}
		return false;
	}


	public boolean startWorkingOnIssue(@NotNull final JIRAIssue issue) {
//		if (issue == null) {
//			return;
//		}
		final ServerData server = issue.getServer();
		boolean isOk = createChangeListAction(issue);

		Task.Backgroundable startWorkOnIssue = new Task.Backgroundable(getProject(), "Starting Work on Issue", false) {

			@Override
			public void run(@NotNull final ProgressIndicator indicator) {
				try {
					if (!issue.getAssigneeId().equals(server.getUserName())) {
						setStatusMessage("Assigning issue " + issue.getKey() + " to me...");
						jiraServerFacade.setAssignee(server, issue, server.getUserName());
					}
					List<JIRAAction> actions = jiraServerFacade.getAvailableActions(server, issue);
					boolean found = false;
					for (JIRAAction a : actions) {
						if (a.getId() == Constants.JiraActionId.START_PROGRESS.getId()) {
							setStatusMessage("Starting progress on " + issue.getKey() + "...");
							jiraServerFacade.progressWorkflowAction(server, issue, a);
							JIRAIssueProgressTimestampCache.getInstance().setTimestamp(server, issue);
							setStatusMessage("Started progress on " + issue.getKey());
							found = true;
							jiraIssueListModelBuilder.reloadIssue(issue.getKey(), server);
							break;
						}
					}
					if (!found && issue.getStatusConstant().getId() != Constants.JiraStatusId.IN_PROGRESS.getId()) {
						setStatusMessage("Progress on "
								+ issue.getKey()
								+ "  not started - no such workflow action available");
					}
				} catch (JIRAException e) {
					setStatusMessage("Error starting progress on issue: " + e.getMessage(), true);
				} catch (NullPointerException e) {
					// eeeem - now what?
				}
			}
		};

		if (isOk) {
			ProgressManager.getInstance().run(startWorkOnIssue);
		}

		return isOk;
	}

	private void refreshFilterModel() {
		try {
			jiraFilterListModelBuilder.rebuildModel(jiraServerModel);
		} catch (JIRAFilterListBuilder.JIRAServerFiltersBuilderException e) {
			//@todo show in message editPane
			setStatusMessage("Some Jira servers did not return saved filters", true);
		}
	}

	public void refreshIssues(final boolean reload) {
		JIRAManualFilter manualFilter = jiraFilterTree.getSelectedManualFilter();
		JIRASavedFilter savedFilter = jiraFilterTree.getSelectedSavedFilter();
		ServerData serverCfg = getSelectedServer();
		if (savedFilter != null) {
			refreshIssues(savedFilter, serverCfg, reload);
		} else if (manualFilter != null) {
			refreshIssues(manualFilter, serverCfg, reload);
		} else if (jiraFilterTree.isRecentlyOpenSelected()) {
			refreshRecenltyOpenIssues(reload);
		}
	}

	private void refreshIssues(final JIRAManualFilter manualFilter, final ServerData jiraServerCfg, final boolean reload) {
		if (WindowManager.getInstance().getIdeFrame(getProject()) == null) {
			return;
		}
		Task.Backgroundable task = new Task.Backgroundable(getProject(), "Retrieving issues", false) {
			@Override
			public void run(@NotNull final ProgressIndicator indicator) {
				try {
					getStatusBarPane().setMessage("Loading issues...", false);
					jiraIssueListModelBuilder.addIssuesToModel(manualFilter, jiraServerCfg,
							pluginConfiguration.getJIRAConfigurationData().getPageSize(), reload);
				} catch (JIRAException e) {
					setStatusMessage(e.getMessage(), true);
				}
			}
		};
		ProgressManager.getInstance().run(task);
	}

	private void refreshIssues(final JIRASavedFilter savedFilter, final ServerData jiraServerCfg, final boolean reload) {
		if (WindowManager.getInstance().getIdeFrame(getProject()) == null) {
			return;
		}
		Task.Backgroundable task = new Task.Backgroundable(getProject(), "Retrieving issues", false) {
			@Override
			public void run(@NotNull final ProgressIndicator indicator) {
				try {
					getStatusBarPane().setMessage("Loading issues...", false);
					jiraIssueListModelBuilder.addIssuesToModel(savedFilter, jiraServerCfg,
							pluginConfiguration.getJIRAConfigurationData().getPageSize(), reload);
				} catch (JIRAException e) {
					setStatusMessage(e.getMessage(), true);
				}
			}
		};
		ProgressManager.getInstance().run(task);
	}


	private void refreshRecenltyOpenIssues(final boolean reload) {
		if (WindowManager.getInstance().getIdeFrame(getProject()) == null) {
			return;
		}
		Task.Backgroundable task = new Task.Backgroundable(getProject(), "Retrieving issues", false) {
			@Override
			public void run(@NotNull final ProgressIndicator indicator) {
				try {
					getStatusBarPane().setMessage("Loading issues...", false);
					jiraIssueListModelBuilder.addRecenltyOpenIssuesToModel(reload);
				} catch (JIRAException e) {
					setStatusMessage(e.getMessage(), true);
				}
			}
		};
		ProgressManager.getInstance().run(task);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void configurationUpdated(final ProjectConfiguration aProjectConfiguration) {
		refreshModels();
	}

	/**
	 * Must be called from dispatch thread
	 */
	public void refreshModels() {
		Task.Backgroundable task = new MetadataFetcherBackgroundableTask();
		ProgressManager.getInstance().run(task);
	}


	public void projectRegistered() {

	}

	public JiraIssueGroupBy getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(JiraIssueGroupBy groupBy) {
		this.groupBy = groupBy;
		issueTreeBuilder.setGroupBy(groupBy);
		issueTreeBuilder.rebuild(getRightTree(), getRightPanel());
		expandAllRightTreeNodes();

		// store in project workspace
		jiraWorkspaceConfiguration.getView().setGroupBy(groupBy);
	}

	public void createIssue() {

		if (jiraIssueListModelBuilder == null) {
			return;
		}

		final ServerData server = getSelectedServer();

		if (server != null) {
			final IssueCreateDialog issueCreateDialog = new IssueCreateDialog(jiraServerModel, server,
					jiraWorkspaceConfiguration);

			issueCreateDialog.initData();
			issueCreateDialog.show();
			if (issueCreateDialog.isOK()) {

				Task.Backgroundable createTask = new Task.Backgroundable(getProject(), "Creating Issue", false) {
					@Override
					public void run(@NotNull final ProgressIndicator indicator) {
						setStatusMessage("Creating new issue...");
						String message;
						boolean isError = false;
						try {
							JIRAIssue issueToCreate = issueCreateDialog.getJIRAIssue();
							final JIRAIssue createdIssue = jiraServerFacade.createIssue(server,
									issueToCreate);

							message = "New issue created: <a href="
									+ createdIssue.getIssueUrl()
									+ ">"
									+ createdIssue.getKey()
									+ "</a>";

							EventQueue.invokeLater(new Runnable() {
								public void run() {
									recentlyOpenIssuesCache.addIssue(createdIssue);
//									jiraIssueListModelBuilder.updateIssue(createdIssue);
									openIssue(createdIssue);
									refreshIssues(true);
								}
							});

						} catch (JIRAException e) {
							message = "Failed to create new issue: " + e.getMessage();
							isError = true;
						}

						final String msg = message;
						setStatusMessage(msg, isError);
					}

				};

				ProgressManager.getInstance().run(createTask);
			}
		}

	}

	public ConfigurationListener getConfigListener() {
		return configListener;
	}

	public boolean isGroupSubtasksUnderParent() {
		return groupSubtasksUnderParent;
	}

	public void setGroupSubtasksUnderParent(boolean state) {
		if (state != groupSubtasksUnderParent) {
			groupSubtasksUnderParent = state;
			issueTreeBuilder.setGroupSubtasksUnderParent(groupSubtasksUnderParent);
			issueTreeBuilder.rebuild(getRightTree(), getRightScrollPane());
			expandAllRightTreeNodes();
			jiraWorkspaceConfiguration.getView().setCollapseSubtasksUnderParent(groupSubtasksUnderParent);
		}
	}

	public JIRAFilterListModel getJIRAFilterListModel() {
		if (jiraFilterListModel == null) {
			jiraFilterListModel = new JIRAFilterListModel();
		}
		return jiraFilterListModel;
	}

	public ServerData getSelectedServer() {
		ServerData server = jiraFilterTree != null ? jiraFilterTree.getSelectedServer() : null;
		if (server != null) {
			return server;
		}
		return projectCfgManager.getDefaultJiraServer();
	}

	public boolean isRecentlyOpenFilterSelected() {
		return jiraFilterTree != null && jiraFilterTree.isRecentlyOpenSelected();
	}

	/**
	 * @return list of recenlty open issues loaded earlier
	 */
	public List<JIRAIssue> getLoadedRecenltyOpenIssues() {
		return new ArrayList<JIRAIssue>(recentlyOpenIssuesCache.getLoadedRecenltyOpenIssues());
	}

	public List<JIRAIssue> loadRecenltyOpenIssues() {
		return new ArrayList<JIRAIssue>(recentlyOpenIssuesCache.loadRecenltyOpenIssues());
	}


	private class MetadataFetcherBackgroundableTask extends Task.Backgroundable {
		private Collection<ServerData> servers = null;
		private boolean refreshIssueList = false;

		/**
		 * Clear server model and refill it with all enabled servers' data
		 */
		public MetadataFetcherBackgroundableTask() {
			super(IssuesToolWindowPanel.this.getProject(), "Retrieving JIRA information", false);
			fillServerData();
			jiraServerModel.clearAll();
			refreshIssueList = true;
		}

		private void fillServerData() {
			servers = new ArrayList<ServerData>();
			for (JiraServerCfg serverCfg : cfgManager.getAllEnabledJiraServers(CfgUtil.getProjectId(getProject()))) {
				servers.add(projectCfgManager.getServerData(serverCfg));
			}
		}

		/**
		 * Add requestes server's data to the server model
		 *
		 * @param server		   server added to the model with all fetched data
		 * @param refreshIssueList refresh issue list
		 */
		public MetadataFetcherBackgroundableTask(final JiraServerCfg server, boolean refreshIssueList) {
			super(IssuesToolWindowPanel.this.getProject(), "Retrieving JIRA information", false);
			this.servers = Arrays.asList(projectCfgManager.getServerData(server));
			this.refreshIssueList = refreshIssueList;
		}

		@Override
		public void run(@NotNull final ProgressIndicator indicator) {

			try {
				jiraServerModel.setModelFrozen(true);

				for (ServerData server : servers) {
					try {
						//returns false if no cfg is available or login failed
						Boolean serverCheck = jiraServerModel.checkServer(server);
						if (serverCheck == null || !serverCheck) {
							setStatusMessage("Unable to connect to server. " + jiraServerModel.getErrorMessage(server),
									true);
							EventQueue.invokeLater(new MissingPasswordHandlerJIRA(jiraServerFacade,
									(JiraServerCfg) cfgManager.getServer(CfgUtil.getProjectId(project), server), project));
							continue;
						}//@todo remove  saved filters download or merge with existing in listModel

						final String serverStr = "[" + server.getName() + "] ";
						setStatusMessage(serverStr + "Retrieving saved filters...");
						jiraServerModel.getSavedFilters(server);
						setStatusMessage(serverStr + "Retrieving projects...");
						jiraServerModel.getProjects(server);
						setStatusMessage(serverStr + "Retrieving issue types...");
						jiraServerModel.getIssueTypes(server, null, true);
						setStatusMessage(serverStr + "Retrieving statuses...");
						jiraServerModel.getStatuses(server);
						setStatusMessage(serverStr + "Retrieving resolutions...");
						jiraServerModel.getResolutions(server, true);
						setStatusMessage(serverStr + "Retrieving priorities...");
						jiraServerModel.getPriorities(server, true);
						setStatusMessage(serverStr + "Retrieving projects...");
						jiraServerModel.getProjects(server);
						setStatusMessage(serverStr + "Server data query finished");
					} catch (RemoteApiException e) {
						setStatusMessage("Unable to connect to server. " + jiraServerModel.getErrorMessage(server), true);
					} catch (JIRAException e) {
						setStatusMessage("Cannot download details:" + e.getMessage(), true);
					}
				}
			} finally {
				// todo it should be probably called in the UI thread as most frozen listeners do something with UI controls
				jiraServerModel.setModelFrozen(false);
			}
		}

		@Override
		public void onSuccess() {
			refreshFilterModel();
			if (refreshIssueList) {
				jiraFilterListModel.fireModelChanged();
			} else {
				jiraFilterListModel.fireServerAdded();
			}
		}
	}

	@Nullable
	public Object getData(@NotNull final String dataId) {
		if (dataId.equals(Constants.ISSUE)) {
			return getSelectedIssue();
		}
		if (dataId.equals(Constants.SERVER)) {
			return getSelectedServer();
		}
		return null;
	}

	private class LocalConfigurationListener extends ConfigurationListenerAdapter {

		@Override
		public void serverConnectionDataChanged(final ServerId serverId) {
			ServerCfg server = cfgManager.getServer(CfgUtil.getProjectId(project), serverId);
			if (server instanceof JiraServerCfg && server.getServerType() == ServerType.JIRA_SERVER) {
				jiraServerModel.clear(server.getServerId());
				Task.Backgroundable task = new MetadataFetcherBackgroundableTask((JiraServerCfg) server, true);
				ProgressManager.getInstance().run(task);
			}
		}

		@Override
		public void serverNameChanged(final ServerId serverId) {
			ServerCfg server = cfgManager.getServer(CfgUtil.getProjectId(project), serverId);
			if (server instanceof JiraServerCfg) {
				jiraServerModel.replace(projectCfgManager.getServerData(server));
				refreshFilterModel();
				jiraFilterListModel.fireServerNameChanged();
			}
		}

		@Override
		public void serverDisabled(final ServerId serverId) {
			ServerCfg server = cfgManager.getServer(CfgUtil.getProjectId(project), serverId);
			if (server instanceof JiraServerCfg && server.getServerType() == ServerType.JIRA_SERVER) {
				removeServer(serverId, recenltyViewedAffected(server));
			}
		}

		@Override
		public void serverRemoved(final ServerCfg oldServer) {
			if (oldServer instanceof JiraServerCfg && oldServer.getServerType() == ServerType.JIRA_SERVER) {
				removeServer(oldServer.getServerId(), recenltyViewedAffected(oldServer));
			}
		}

		@Override
		public void serverEnabled(final ServerId serverId) {
			ServerCfg server = cfgManager.getServer(CfgUtil.getProjectId(project), serverId);
			addServer(server, recenltyViewedAffected(server));
		}

		@Override
		public void serverAdded(final ServerCfg newServer) {
			addServer(newServer, false);
		}

		private void addServer(final ServerCfg server, boolean refreshIssueList) {
			if (server instanceof JiraServerCfg && server.getServerType() == ServerType.JIRA_SERVER) {
				Task.Backgroundable task = new MetadataFetcherBackgroundableTask((JiraServerCfg) server, refreshIssueList);
				ProgressManager.getInstance().run(task);

			}
		}

		private void removeServer(final ServerId serverId, final boolean reloadIssueList) {
			jiraServerModel.clear(serverId);
			refreshFilterModel();
			jiraFilterListModel.fireServerRemoved();

			if (reloadIssueList) {
				refreshRecenltyOpenIssues(true);
			}
		}

		private boolean recenltyViewedAffected(final ServerCfg server) {
			if (server instanceof JiraServerCfg && server.getServerType() == ServerType.JIRA_SERVER
					&& jiraFilterTree.isRecentlyOpenSelected()) {
				// check if some recenlty open issue come from enabled server; if yes then return true
				JiraWorkspaceConfiguration conf = IdeaHelper.getProjectComponent(project, JiraWorkspaceConfiguration.class);
				if (conf != null) {
					final Collection<IssueRecentlyOpenBean> recentlyOpen = conf.getRecentlyOpenIssues();
					if (recentlyOpen != null) {
						for (IssueRecentlyOpenBean i : recentlyOpen) {
							if (i.getServerId().equals(server.getServerId().toString())) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}

	}

	@Override
	public JTree createRightTree() {
		JiraIssueListTree issueTree = new JiraIssueListTree();

		new TreeSpeedSearch(issueTree);

		issueTreeBuilder.rebuild(issueTree, getRightPanel());
		return issueTree;
	}

	@Override
	public JiraIssueListTree getRightTree() {
		return (JiraIssueListTree) super.getRightTree();
	}

	@Override
	public JTree createLeftTree() {
		if (jiraFilterTree == null) {
			jiraFilterTree = new JIRAFilterTree(jiraWorkspaceConfiguration, getJIRAFilterListModel());
		}

		return jiraFilterTree;
	}

	@Override
	public String getActionPlaceName() {
		return PLACE_PREFIX + this.getProject().getName();
	}

	private class LocalJiraFilterTreeSelectionListener implements JiraFilterTreeSelectionListener {

		public void selectedSavedFilterNode(final JIRASavedFilter savedFilter, final ServerData jiraServerCfg) {
			hideManualFilterPanel();
			refreshIssues(savedFilter, jiraServerCfg, true);
			jiraWorkspaceConfiguration.getView().setViewServerId(jiraServerCfg.getServerId());
			jiraWorkspaceConfiguration.getView().setViewFilterId(Long.toString(savedFilter.getId()));
		}

		public void selectedManualFilterNode(final JIRAManualFilter manualFilter, final ServerData jiraServerCfg) {
			showManualFilterPanel(manualFilter, jiraServerCfg);
			jiraWorkspaceConfiguration.getView().setViewServerId(jiraServerCfg.getServerId());
			jiraWorkspaceConfiguration.getView().setViewFilterId(JiraFilterConfigurationBean.MANUAL_FILTER);

			refreshIssues(manualFilter, jiraServerCfg, true);
		}

		public void selectionCleared() {
			hideManualFilterPanel();

			enableGetMoreIssues(false);

			jiraWorkspaceConfiguration.getView().setViewServerId("");
			jiraWorkspaceConfiguration.getView().setViewFilterId("");

			jiraIssueListModelBuilder.reset();
		}

		public void selectedRecentlyOpenNode() {
			hideManualFilterPanel();

			// refresh issues view
			refreshRecenltyOpenIssues(true);

			jiraWorkspaceConfiguration.getView().setViewServerId("");
			jiraWorkspaceConfiguration.getView().setViewFilterId(JiraFilterConfigurationBean.RECENTLY_OPEN_FILTER);
		}
	}

	private class LogWorkWorkerTask extends Task.Backgroundable {
		private final JIRAIssue issue;
		private final WorkLogCreateAndMaybeDeactivateDialog dialog;
		private final ServerData jiraServer;
		private final boolean deactivateIssue;

		public LogWorkWorkerTask(JIRAIssue issue, WorkLogCreateAndMaybeDeactivateDialog dialog,
				ServerData jiraServer, boolean deactivateIssue) {
			super(IssuesToolWindowPanel.this.getProject(),
					deactivateIssue ? "Stopping Work" : "Logging Work", false);

			this.issue = issue;
			this.dialog = dialog;
			this.jiraServer = jiraServer;
			this.deactivateIssue = deactivateIssue;
		}

		@Override
		public void run(@NotNull final ProgressIndicator indicator) {
			try {

				if (jiraServer != null) {
					if (dialog.isLogTime()) {
						setStatusMessage("Logging work for issue " + issue.getKey() + "...");
						Calendar cal = Calendar.getInstance();
						cal.setTime(dialog.getStartDate());

						String newRemainingEstimate = dialog.getUpdateRemainingManually()
								? dialog.getRemainingEstimateString() : null;
						jiraServerFacade.logWork(jiraServer, issue, dialog.getTimeSpentString(),
								cal, null,
								!dialog.getLeaveRemainingUnchanged(), newRemainingEstimate);
						JIRAIssueProgressTimestampCache.getInstance().setTimestamp(
								jiraServer, issue);
						setStatusMessage("Logged work for issue " + issue.getKey());
					}
					if (deactivateIssue) {
						JIRAAction stopProgressAction = null;
						setStatusMessage("Checking workflow actions for issue " + issue.getKey() + "...");

						List<JIRAAction> actions = jiraServerFacade.getAvailableActions(
								jiraServer, issue);
						for (JIRAAction a : actions) {
							if (a.getId() == Constants.JiraActionId.STOP_PROGRESS.getId()) {
								List<JIRAActionField> fields =
										jiraServerFacade.getFieldsForAction(
												jiraServer, issue, a);
								if (fields.isEmpty()) {
									stopProgressAction = a;
								}
								break;
							}
						}
						if (stopProgressAction != null) {
							setStatusMessage("Stopping work for issue " + issue.getKey() + "...");
							jiraServerFacade.progressWorkflowAction(jiraServer, issue, stopProgressAction);
						}

						if (dialog.isCommitChanges()) {
							final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
							final LocalChangeList list = dialog.getCurrentChangeList();
							list.setComment(dialog.getComment());

							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									setStatusMessage("Committing changes...");
									changeListManager.commitChanges(list, dialog.getSelectedChanges());
									setStatusMessage("Deactivated issue " + issue.getKey());
								}
							});

							if (dialog.isDeactivateCurrentChangeList()) {
								List<LocalChangeList> chLists = changeListManager.getChangeLists();
								for (LocalChangeList chl : chLists) {
									if ("Default".equals(chl.getName())) {
										changeListManager.setDefaultChangeList(chl);
										break;
									}
								}
							}
						}

						setStatusMessage("Deactivated issue " + issue.getKey());
						jiraIssueListModelBuilder.reloadIssue(issue.getKey(), jiraServer);
					}
				}
			} catch (JIRAException e) {
				if (deactivateIssue) {
					setStatusMessage("Issue not deactivated: " + e.getMessage(), true);
				} else {
					setStatusMessage("Work not logged: " + e.getMessage(), true);
				}
			}
		}
	}


	private class LocalJiraIssueListModelListener implements JIRAIssueListModelListener {
		public void modelChanged(JIRAIssueListModel model) {
			SwingUtilities.invokeLater(new ModelChangedRunnable());
		}

		public void issuesLoaded(JIRAIssueListModel model, int loadedIssues) {
			if (loadedIssues >= pluginConfiguration.getJIRAConfigurationData().getPageSize()) {
				enableGetMoreIssues(true);
				setStatusMessage("Loaded " + loadedIssues + " issues", false, true);
			} else {
				enableGetMoreIssues(false);
				setStatusMessage("Loaded " + loadedIssues + " issues", false, false);
			}
		}

		private class ModelChangedRunnable implements Runnable {
			public void run() {
				JiraIssueAdapter.clearCache();
				ServerData srvcfg = getSelectedServer();
				if (srvcfg == null && !isRecentlyOpenFilterSelected()) {
					setStatusMessage("Nothing selected", false, false);
					issueTreeBuilder.rebuild(getRightTree(), getRightScrollPane());
				} else if (srvcfg == null && isRecentlyOpenFilterSelected()) {
					Map<Pair<String, ServerId>, String> projects = new HashMap<Pair<String, ServerId>, String>();
					for (JiraServerCfg server : projectCfgManager.getCfgManager()
							.getAllEnabledJiraServers(CfgUtil.getProjectId(project))) {
						try {
							for (JIRAProject p : jiraServerModel.getProjects(projectCfgManager.getServerData(server))) {
								projects.put(new Pair<String, ServerId>(p.getKey(), server.getServerId()), p.getName());
							}
						} catch (JIRAException e) {
							setStatusMessage("Cannot retrieve projects." + e.getMessage(), true);
						}
					}
					issueTreeBuilder.setProjectKeysToNames(projects);
					issueTreeBuilder.rebuild(getRightTree(), getRightScrollPane());
				} else if (srvcfg != null) {
					Map<Pair<String, ServerId>, String> projectMap = new HashMap<Pair<String, ServerId>, String>();
					try {
						for (JIRAProject p : jiraServerModel.getProjects(srvcfg)) {
							projectMap.put(new Pair<String, ServerId>(p.getKey(), new ServerId(srvcfg.getServerId())),
									p.getName());
						}
					} catch (JIRAException e) {
						setStatusMessage("Cannot retrieve projects." + e.getMessage(), true);
					}
					issueTreeBuilder.setProjectKeysToNames(projectMap);
					issueTreeBuilder.rebuild(getRightTree(), getRightScrollPane());
					expandAllRightTreeNodes();
				}
			}
		}
	}
}
