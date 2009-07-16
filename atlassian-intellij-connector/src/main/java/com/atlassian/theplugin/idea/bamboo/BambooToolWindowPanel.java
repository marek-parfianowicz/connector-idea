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
package com.atlassian.theplugin.idea.bamboo;

import com.atlassian.theplugin.cfg.CfgUtil;
import com.atlassian.theplugin.commons.bamboo.BambooBuild;
import com.atlassian.theplugin.commons.bamboo.BambooServerData;
import com.atlassian.theplugin.commons.bamboo.BambooServerFacadeImpl;
import com.atlassian.theplugin.commons.bamboo.BambooStatusChecker;
import com.atlassian.theplugin.commons.cfg.ConfigurationListenerAdapter;
import com.atlassian.theplugin.commons.cfg.ProjectConfiguration;
import com.atlassian.theplugin.commons.exception.ServerPasswordNotProvidedException;
import com.atlassian.theplugin.commons.remoteapi.RemoteApiException;
import com.atlassian.theplugin.commons.remoteapi.ServerData;
import com.atlassian.theplugin.configuration.BambooWorkspaceConfiguration;
import com.atlassian.theplugin.configuration.WorkspaceConfigurationBean;
import com.atlassian.theplugin.idea.Constants;
import com.atlassian.theplugin.idea.IdeaHelper;
import com.atlassian.theplugin.idea.ThePluginProjectComponent;
import com.atlassian.theplugin.idea.bamboo.tree.BuildTree;
import com.atlassian.theplugin.idea.bamboo.tree.BuildTreeModel;
import com.atlassian.theplugin.idea.config.ProjectCfgManagerImpl;
import com.atlassian.theplugin.idea.ui.DialogWithDetails;
import com.atlassian.theplugin.idea.ui.PopupAwareMouseAdapter;
import com.atlassian.theplugin.util.PluginUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.SearchTextField;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Wojciech Seliga
 */
public class BambooToolWindowPanel extends ThreePanePanel implements DataProvider {

	public static final String PLACE_PREFIX = BambooToolWindowPanel.class.getSimpleName();
	private static final String COMPLETED_BUILDS = "Completed Builds";
	private final Project project;
	private final BuildListModelImpl bambooModel;
	private ProjectCfgManagerImpl projectCfgManager;
	private final BuildTree buildTree;
	private final BambooFilterList filterList;
	private SearchTextField searchField = new SearchTextField();
	private JComponent leftToolBar;
	private JComponent rightToolBar;
	private BambooWorkspaceConfiguration bambooConfiguration;
	private BuildGroupBy groupBy = BuildGroupBy.NONE;
	private SearchBuildListModel searchBuildModel;
	private BuildHistoryPanel buildHistoryPanel;
	private JLabel planHistoryListLabel;

	public BambooFilterType getBambooFilterType() {
		return filterList.getBambooFilterType();
	}

	public BambooToolWindowPanel(@NotNull final Project project,
			@NotNull final BuildListModelImpl bambooModel,
			@NotNull final WorkspaceConfigurationBean projectConfiguration,
			@NotNull final ProjectCfgManagerImpl projectCfgManager) {

		this.project = project;
		this.bambooModel = bambooModel;
		this.projectCfgManager = projectCfgManager;
		this.bambooConfiguration = projectConfiguration.getBambooConfiguration();

		filterList = new BambooFilterList(projectCfgManager, bambooModel);
		projectCfgManager.addProjectConfigurationListener(new ConfigurationListenerAdapter() {
			@Override
			public void bambooServersChanged(final ProjectConfiguration newConfiguration) {
				filterList.update();
			}
		});

		filterList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					final BambooBuildFilter filter = filterList.getSelection();
					bambooModel.setFilter(filter);
				}
			}
		});

		bambooModel.addListener(new BuildListModelListener() {
			public void modelChanged() {
			}

			public void generalProblemsHappened(@Nullable Collection<Exception> generalExceptions) {
				if (generalExceptions != null && generalExceptions.size() > 0) {
					Exception e = generalExceptions.iterator().next();
					setErrorMessage(e.getMessage(), e);
				}
			}

			public void buildsChanged(@Nullable final Collection<String> additionalInfo,
					@Nullable final Collection<Pair<String, Throwable>> errors) {

				// we do not support multiple messages in status bar yet (waiting for inbox to be implemented)
				if (errors != null && !errors.isEmpty()) {

					// get last error message
					Pair<String, Throwable> error = (Pair<String, Throwable>) errors.toArray()[errors.size() - 1];

					setErrorMessage(error.getFirst(), error.getSecond());
				} else if (additionalInfo != null && !additionalInfo.isEmpty()) {
					setStatusMessage(additionalInfo.toArray(new String[1])[additionalInfo.size() - 1]);
				}
				filterList.update();
			}
		});

		searchBuildModel = new SearchBuildListModel(bambooModel);
		buildTree = new BuildTree(groupBy, new BuildTreeModel(searchBuildModel), getRightScrollPane());
		leftToolBar = createLeftToolBar();
		rightToolBar = createRightToolBar();
		buildHistoryPanel = new BuildHistoryPanel(project);

		init();
		addBuildTreeListeners();
		addSearchBoxListener();

		// restore GroupBy and FilterBy setting
		if (bambooConfiguration != null && bambooConfiguration.getView() != null) {
			if (bambooConfiguration.getView().getGroupBy() != null) {
				groupBy = bambooConfiguration.getView().getGroupBy();
				setGroupingType(groupBy);
			}
			if (bambooConfiguration.getView().getFilterType() != null) {
				setBambooFilterType(bambooConfiguration.getView().getFilterType());
			}
		}

		setLeftPaneVisible(filterList.getBambooFilterType() != null);
	}

	private void addBuildTreeListeners() {
		buildTree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				final BambooBuildAdapterIdea buildDetailsInfo = buildTree.getSelectedBuild();
				if (e.getKeyCode() == KeyEvent.VK_ENTER && buildDetailsInfo != null) {
					openBuild(buildDetailsInfo);
				}
			}
		});

		buildTree.addMouseListener(new PopupAwareMouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				final BambooBuildAdapterIdea buildDetailsInfo = buildTree.getSelectedBuild();
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && buildDetailsInfo != null) {
					openBuild(buildDetailsInfo);
				}
			}

			@Override
			protected void onPopup(MouseEvent e) {
				int selRow = buildTree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = buildTree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1 && selPath != null) {
					buildTree.setSelectionPath(selPath);
					final BambooBuildAdapterIdea buildDetailsInfo = buildTree.getSelectedBuild();
					if (buildDetailsInfo != null) {
						launchContextMenu(e);
					}
				}
			}
		});

		buildTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent event) {
				final BambooBuildAdapterIdea buildDetailsInfo = buildTree.getSelectedBuild();
				if (buildDetailsInfo != null) {
					buildHistoryPanel.showHistoryForBuild(buildDetailsInfo.getBuild());
				} else {
					buildHistoryPanel.clearBuildHistory();
				}
			}
		});
	}

	private void launchContextMenu(MouseEvent e) {
		final DefaultActionGroup actionGroup = new DefaultActionGroup();
		final ActionGroup configActionGroup = (ActionGroup) ActionManager
				.getInstance().getAction("ThePlugin.Bamboo.BuildPopupMenuNew");
		actionGroup.addAll(configActionGroup);

		final ActionPopupMenu popup = ActionManager.getInstance().createActionPopupMenu(getActionPlaceName(), actionGroup);

		final JPopupMenu jPopupMenu = popup.getComponent();
		jPopupMenu.show(e.getComponent(), e.getX(), e.getY());
	}

	private String getActionPlaceName() {
		return PLACE_PREFIX + project.getName();
	}

	public void openBuild(final BambooBuildAdapterIdea buildDetailsInfo) {
		if (buildDetailsInfo != null && buildDetailsInfo.isBamboo2()
				&& buildDetailsInfo.areActionsAllowed()) {
			IdeaHelper.getBuildToolWindow(project).showBuild(buildDetailsInfo);
		}
	}


	public void openBuild(final String buildKey, int buildNumber, final String serverUrl) {

		final Collection<ServerData> servers = new ArrayList<ServerData>(projectCfgManager.getAllBambooServerss());

		ServerData server = CfgUtil.findServer(serverUrl, servers);

		if (server != null && server instanceof BambooServerData) {
			openBuild(buildKey, buildNumber, (BambooServerData) server);
		} else {
			Messages.showInfoMessage(project, "Server " + serverUrl + " not found in configuration.", PluginUtil.PRODUCT_NAME);
		}

	}

	private void openBuild(final String buildKey, final int buildNumber, final BambooServerData server) {
		BambooBuildAdapterIdea build = null;

		for (BambooBuildAdapterIdea b : bambooModel.getAllBuilds()) {
			if (b.getBuild().getPlanKey().equals(buildKey)
					&& b.getNumber() == buildNumber
					&& b.getServer().getServerId().equals(server.getServerId())) {
				build = b;
				break;
			}
		}

		if (build != null) {
			openBuild(build);
		} else {
			Task.Modal task = new Task.Modal(project, "Fetching build " + buildKey + "-" + buildNumber, false) {
				private BambooBuildAdapterIdea build;
				private Throwable exception;

				@Override
				public void run(@NotNull ProgressIndicator progressIndicator) {
					progressIndicator.setIndeterminate(true);
					try {
						build = new BambooBuildAdapterIdea(BambooServerFacadeImpl.getInstance(PluginUtil.getLogger()).
								getBuildForPlanAndNumber(server, buildKey, buildNumber, server.getTimezoneOffset()));
					} catch (RemoteApiException e) {
						exception = e;
					} catch (ServerPasswordNotProvidedException e) {
						exception = e;
					}
				}

				@Override
				public void onSuccess() {
					if (getProject().isDisposed()) {
						return;
					}
					if (exception != null) {
						DialogWithDetails.showExceptionDialog(project, "Cannot fetch build "
								+ buildKey + "-" + buildNumber + " from server " + server.getName(), exception);
						return;
					}
					if (build != null) {
						openBuild(build);
					}
				}
			};
			task.queue();
		}
	}

	public void refresh() {
		final ThePluginProjectComponent currentProject = IdeaHelper.getCurrentProjectComponent(project);
		if (currentProject == null) {
			return;
		}

		final BambooStatusChecker checker = currentProject.getBambooStatusChecker();

		if (checker.canSchedule()) {
			Task.Backgroundable refresh = new Task.Backgroundable(project, "Refreshing Bamboo Panel", false) {
				@Override
				public void run(@NotNull final ProgressIndicator indicator) {
					checker.newTimerTask().run();
				}
			};

			ProgressManager.getInstance().run(refresh);
		}
	}

	public BambooBuild getSelectedHistoryBuild() {
		return buildHistoryPanel.getSelectedBuild();
	}

	protected void addSearchBoxListener() {
		searchField.addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				searchBuildModel.setSearchTerm(getSearchField().getText());
			}

			public void removeUpdate(DocumentEvent e) {
				searchBuildModel.setSearchTerm(getSearchField().getText());
			}

			public void changedUpdate(DocumentEvent e) {
				searchBuildModel.setSearchTerm(getSearchField().getText());
			}
		});

		searchField.addKeyboardListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					searchField.addCurrentTextToHistory();
				}
			}

			public void keyReleased(KeyEvent e) {
			}
		});
	}

	private SearchTextField getSearchField() {
		return searchField;
	}

	@Override
	public JTree getRightTree() {
		return buildTree;
	}

	protected JComponent getRightMostPanel() {
		return buildHistoryPanel;
	}

	protected JComponent getRightMostToolBar() {
		return rightToolBar;
	}

	public Object getData(@NonNls final String dataId) {
		if (dataId.equals(Constants.SERVER)) {
			// return server of selected build
			if (buildTree.getSelectedBuild() != null) {
				return buildTree.getSelectedBuild().getServer();
			}

			final BambooBuildFilter filter = filterList.getSelection();

			// return server of selected filter in case of server filtering
			if (getBambooFilterType() == BambooFilterType.SERVER
					&& filter != null && filter instanceof BambooCompositeOrFilter) {

				BambooCompositeOrFilter filterImpl = (BambooCompositeOrFilter) filter;

				Collection<BambooBuildFilter> filters = filterImpl.getFilters();
				for (BambooBuildFilter buildFilter : filters) {
					if (buildFilter instanceof BambooFilterList.BambooServerFilter) {
						BambooFilterList.BambooServerFilter serverFilter =
								(BambooFilterList.BambooServerFilter) buildFilter;

						return serverFilter.getBambooServerCfg();
					}
				}
			}
		}

		return null;
	}

	public Collection<BambooServerData> getServers() {
		return projectCfgManager.getAllEnabledBambooServerss();
	}

	public void setGroupingType(@NonNls final BuildGroupBy groupingType) {
		if (groupingType != null) {
			this.groupBy = groupingType;
			buildTree.groupBy(groupingType);
			bambooConfiguration.getView().setGroupBy(groupingType);
		}
	}

	public void setBambooFilterType(@Nullable final BambooFilterType bambooFilterType) {
		filterList.setBambooFilterType(bambooFilterType);
		setLeftPaneVisible(filterList.getBambooFilterType() != null);
		bambooModel.setFilter(null);
		// by default there should be "ALL", which means null filter
//		buildTree.updateModel(bambooModel.getBuilds());

		bambooConfiguration.getView().setFilterType(bambooFilterType);
	}

	@Override
	protected JComponent getLeftPanel() {
		return filterList;
	}

	@Override
	protected JComponent getLeftToolBar() {
		return leftToolBar;
	}

	private JComponent createLeftToolBar() {
		final JPanel toolBarPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		toolBarPanel.add(loadToolBar("ThePlugin.Bamboo.LeftToolBar"), gbc);
		gbc.gridx++;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.FIRST_LINE_END;
		searchField.setMinimumSize(searchField.getPreferredSize());
		toolBarPanel.add(searchField, gbc);

		return toolBarPanel;
	}

	private JComponent createRightToolBar() {
		final JPanel toolBarPanel = new JPanel(new FormLayout("pref, fill:pref:grow, 3dlu", "pref, 3dlu, pref, 3dlu"));
		CellConstraints cc = new CellConstraints();

		toolBarPanel.add(loadToolBar("ThePlugin.Bamboo.RightToolBar"), cc.xyw(1, 1, 2));
		planHistoryListLabel = new JLabel(COMPLETED_BUILDS);
		toolBarPanel.add(planHistoryListLabel, cc.xy(1, 3));
		toolBarPanel.add(new JSeparator(SwingConstants.HORIZONTAL), cc.xy(2, 3));
		return toolBarPanel;
	}

	@Nullable
	private JComponent loadToolBar(final String toolbarName) {
		ActionManager actionManager = ActionManager.getInstance();
		ActionGroup toolbar = (ActionGroup) actionManager.getAction(toolbarName);
		if (toolbar != null) {
			final ActionToolbar actionToolbar =
					actionManager.createActionToolbar(PLACE_PREFIX + project.getName(), toolbar, true);
			actionToolbar.setTargetComponent(this);
			return actionToolbar.getComponent();
		}
		return null;
	}

	public BuildGroupBy getGroupBy() {
		return groupBy;
	}

	public BambooBuildAdapterIdea getSelectedBuild() {
		return buildTree.getSelectedBuild();
	}
}
