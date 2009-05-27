package com.atlassian.theplugin.idea.crucible;

import com.atlassian.theplugin.commons.cfg.CrucibleServerCfg;
import com.atlassian.theplugin.configuration.CrucibleViewConfigurationBean;
import com.atlassian.theplugin.idea.ui.KeyPressGobbler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class SearchReviewDialog extends DialogWrapper {
	private JPanel rootPane;
	private JTextField ctrlReviewSearch;
	private JPanel ctrlServersPanel;
	private JLabel ctrlLocalInfoLabel;
	private Collection<CrucibleServerCfg> selectedServers = new HashSet<CrucibleServerCfg>();
	private CrucibleViewConfigurationBean crucibleViewConfiguration;

	public Collection<CrucibleServerCfg> getSelectedServers() {
		return selectedServers;
	}

	public SearchReviewDialog(Project project, final Collection<CrucibleServerCfg> servers,
			final CrucibleViewConfigurationBean crucibleViewConfiguration) {
		super(project, true);
		this.crucibleViewConfiguration = crucibleViewConfiguration;

		$$$setupUI$$$();
		init();
		pack();

		setTitle("Search Review");
		getOKAction().putValue(Action.NAME, "Search");
		getOKAction().setEnabled(false);

		addServersCheckboxes(servers);
		if (selectedServers.size() != 0) {
			ctrlLocalInfoLabel.setVisible(false);
		}

		ctrlReviewSearch.addKeyListener(new KeyAdapter() {
			public void keyReleased(final KeyEvent e) {
				if (ctrlReviewSearch.getText().length() == 0) {
					getOKAction().setEnabled(false);
				} else {
					getOKAction().setEnabled(true);
				}
			}
		});
		KeyPressGobbler.gobbleKeyPress(ctrlReviewSearch);
		setOKActionEnabled(false);
	}

	protected void doOKAction() {

		if (crucibleViewConfiguration != null) {

			Collection<String> searchServers = new ArrayList<String>();

			for (CrucibleServerCfg server : selectedServers) {
				searchServers.add(server.getServerId().toString());
			}
			crucibleViewConfiguration.setSearchServers(searchServers);
		}

		super.doOKAction();
//		close(OK_EXIT_CODE);
	}

	public JComponent getPreferredFocusedComponent() {
		return ctrlReviewSearch;
	}

	private void addServersCheckboxes(final Collection<CrucibleServerCfg> servers) {

		ctrlServersPanel.setLayout(new BoxLayout(ctrlServersPanel, BoxLayout.Y_AXIS));

		if (servers != null) {
			for (final CrucibleServerCfg server : servers) {
				final CrucibleServerCheckbox checkbox = new CrucibleServerCheckbox(server);
				ctrlServersPanel.add(checkbox);

				if (crucibleViewConfiguration != null && crucibleViewConfiguration.getSearchServers() != null
						&& crucibleViewConfiguration.getSearchServers().contains(server.getServerId().toString())) {
					checkbox.setSelected(true);
					selectedServers.add(server);
				}

				checkbox.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent e) {
						if (checkbox.isSelected()) {
							selectedServers.add(server);
							ctrlLocalInfoLabel.setVisible(false);
						} else {
							selectedServers.remove(server);
							if (selectedServers.size() == 0) {
								ctrlLocalInfoLabel.setVisible(true);
								pack();
							}
						}
					}
				});
			}
		}
	}

	@Nullable
	protected JComponent createCenterPanel() {
		return getRootComponent();
	}

	private JComponent getRootComponent() {
		return rootPane;
	}

	public String getSearchKey() {
		return ctrlReviewSearch.getText();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		rootPane = new JPanel();
		rootPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 0, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 10, 0), -1, -1));
		rootPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JLabel label1 = new JLabel();
		label1.setHorizontalAlignment(4);
		label1.setHorizontalTextPosition(0);
		label1.setText("Server(s): ");
		label1.setVerticalAlignment(0);
		label1.setVerticalTextPosition(0);
		panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		ctrlReviewSearch = new JTextField();
		panel1.add(ctrlReviewSearch, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST,
				GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
				new Dimension(150, -1), null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setHorizontalAlignment(4);
		label2.setText("Review Key: ");
		panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		ctrlServersPanel = new JPanel();
		ctrlServersPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(ctrlServersPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		ctrlLocalInfoLabel = new JLabel();
		ctrlLocalInfoLabel.setText("No selection. Only local list of reviews will be searched");
		ctrlLocalInfoLabel.setVerticalAlignment(0);
		rootPane.add(ctrlLocalInfoLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return rootPane;
	}


	private static class CrucibleServerCheckbox extends JCheckBox {
		private CrucibleServerCfg server;

		public CrucibleServerCheckbox(CrucibleServerCfg server) {
			super(server.getName());
			this.server = server;
		}

		public CrucibleServerCfg getServer() {
			return server;
		}
	}
}
