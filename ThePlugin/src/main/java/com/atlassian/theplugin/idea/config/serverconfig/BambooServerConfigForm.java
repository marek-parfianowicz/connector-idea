package com.atlassian.theplugin.idea.config.serverconfig;

import com.atlassian.theplugin.bamboo.BambooServerFactory;
import com.atlassian.theplugin.bamboo.api.BambooLoginException;
import com.atlassian.theplugin.configuration.ServerBean;
import com.atlassian.theplugin.util.Util;
import com.intellij.openapi.ui.Messages;
import static com.intellij.openapi.ui.Messages.showMessageDialog;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Plugin configuration form.
 */
public class BambooServerConfigForm extends AbstractServerPanel {
	private JPanel rootComponent;
	private JTextField serverName;
	private JTextField serverUrl;
	private JTextField username;
	private JPasswordField password;
	private JButton testConnection;
	private JCheckBox chkPasswordRemember;
	private JCheckBox cbEnabled;
	private JCheckBox cbUseFavuriteBuilds;
	private JPanel buildsPanel;

	private transient ServerBean server;
	private PlanCheckboxList planList;

	public BambooServerConfigForm() {

		$$$setupUI$$$();
		testConnection.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				try {
					BambooServerFactory.getBambooServerFacade().testServerConnection(serverUrl.getText(),
							username.getText(), String.valueOf(password.getPassword()));
					showMessageDialog("Connected successfully", "Connection OK", Messages.getInformationIcon());
				} catch (BambooLoginException e1) {
					showMessageDialog(e1.getMessage(), "Connection Error", Messages.getErrorIcon());
					//throw e1;
				}
			}
		});
		cbUseFavuriteBuilds.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				planList.setEnabled(!cbUseFavuriteBuilds.isSelected());
				getPlansFromServer(server);
			}
		});
	}

	private void getPlansFromServer(ServerBean aServer) {
		if (!cbUseFavuriteBuilds.isSelected()) {
			planList.setBuilds(aServer);
		}
	}

	public void setData(ServerBean aServer) {
		this.server = new ServerBean(aServer);

		serverName.setText(aServer.getName());
		serverUrl.setText(aServer.getUrlString());
		username.setText(aServer.getUserName());
		chkPasswordRemember.setSelected(aServer.getShouldPasswordBeStored());
		password.setText(aServer.getPasswordString());
		cbEnabled.setSelected(aServer.getEnabled());
		cbUseFavuriteBuilds.setSelected(aServer.getUseFavourite());
		planList.setEnabled(!aServer.getUseFavourite());
		getPlansFromServer(server);
	}

	public ServerBean getData() {

		serverUrl.setText(Util.addHttpPrefix(serverUrl.getText()));

		server.setName(serverName.getText());
		server.setUrlString(serverUrl.getText());
		server.setUserName(username.getText());
		server.setPasswordString(String.valueOf(password.getPassword()), chkPasswordRemember.isSelected());
		server.setEnabled(cbEnabled.isSelected());
		server.setUseFavourite(cbUseFavuriteBuilds.isSelected());
		server.setSubscribedPlansData(planList.getSubscribedPlans());
		return server;
	}

	public boolean isModified() {
		boolean isModified = false;

		if (server != null) {
			if (chkPasswordRemember.isSelected() != server.getShouldPasswordBeStored()) {
				return true;
			}
			if (cbEnabled.isSelected() != server.getEnabled()) {
				return true;
			}
			if (cbUseFavuriteBuilds.isSelected() != server.getUseFavourite()) {
				return true;
			}
			if (serverName.getText() != null
					? !serverName.getText().equals(server.getName()) : server.getName() != null) {
				return true;
			}
			if (serverUrl.getText() != null
					? !serverUrl.getText().equals(server.getUrlString()) : server.getUrlString() != null) {
				return true;
			}
			if (username.getText() != null
					? !username.getText().equals(server.getUserName()) : server.getUserName() != null) {
				return true;
			}
			String pass = String.valueOf(password.getPassword());
			if (!pass.equals(server.getPasswordString())) {
				return true;
			}
			if (planList.isModified()) {
				return true;
			}
		}
		return isModified;
	}

	public JComponent getRootComponent() {
		return rootComponent;
	}

	public void setVisible(boolean visible) {
		rootComponent.setVisible(visible);
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		rootComponent = new JPanel();
		rootComponent.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
		rootComponent.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		serverName = new JTextField();
		serverName.setText("");
		panel1.add(serverName, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label1 = new JLabel();
		label1.setText("Server Name");
		label1.setDisplayedMnemonic('S');
		label1.setDisplayedMnemonicIndex(0);
		panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		serverUrl = new JTextField();
		panel1.add(serverUrl, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		username = new JTextField();
		panel1.add(username, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		password = new JPasswordField();
		password.setText("");
		panel1.add(password, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setText("Server URL");
		label2.setDisplayedMnemonic('U');
		label2.setDisplayedMnemonicIndex(7);
		panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText("User Name");
		label3.setDisplayedMnemonic('N');
		label3.setDisplayedMnemonicIndex(5);
		panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText("Password");
		label4.setDisplayedMnemonic('P');
		label4.setDisplayedMnemonicIndex(0);
		panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		testConnection = new JButton();
		testConnection.setText("Test Connection");
		testConnection.setMnemonic('T');
		testConnection.setDisplayedMnemonicIndex(0);
		panel1.add(testConnection, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		buildsPanel = new JPanel();
		buildsPanel.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
		panel1.add(buildsPanel, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buildsPanel.setBorder(BorderFactory.createTitledBorder("Build plans"));
		cbUseFavuriteBuilds = new JCheckBox();
		cbUseFavuriteBuilds.setText("Use Favourite Builds For Server");
		cbUseFavuriteBuilds.setMnemonic('F');
		cbUseFavuriteBuilds.setDisplayedMnemonicIndex(4);
		buildsPanel.add(cbUseFavuriteBuilds, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JScrollPane scrollPane1 = new JScrollPane();
		buildsPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		planList = new PlanCheckboxList();
		scrollPane1.setViewportView(planList);
		chkPasswordRemember = new JCheckBox();
		chkPasswordRemember.setSelected(true);
		chkPasswordRemember.setText("Remember password");
		chkPasswordRemember.setMnemonic('R');
		chkPasswordRemember.setDisplayedMnemonicIndex(0);
		panel1.add(chkPasswordRemember, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		rootComponent.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		cbEnabled = new JCheckBox();
		cbEnabled.setHorizontalAlignment(4);
		cbEnabled.setHorizontalTextPosition(10);
		cbEnabled.setText("Server Enabled");
		cbEnabled.setMnemonic('E');
		cbEnabled.setDisplayedMnemonicIndex(7);
		panel2.add(cbEnabled, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		label1.setLabelFor(serverName);
		label2.setLabelFor(serverUrl);
		label3.setLabelFor(username);
		label4.setLabelFor(password);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return rootComponent;
	}
}
