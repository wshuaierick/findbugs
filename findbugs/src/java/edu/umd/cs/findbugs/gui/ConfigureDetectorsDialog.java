/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * ConfigureDetectorsDialog.java
 *
 * Created on June 3, 2003, 3:52 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Configure Detectors by enabling/disabling them.
 *
 * @author David Hovemeyer
 */
public class ConfigureDetectorsDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;

	private static final int SPEED_COLUMN = 1;
	private static final int ENABLED_COLUMN = 2;

	/**
	 * Creates new form ConfigureDetectorsDialog
	 */
	public ConfigureDetectorsDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
		postInitComponents();
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        detectorTableScrollPane = new javax.swing.JScrollPane();
        detectorTable = new javax.swing.JTable();
        detectorDescriptionScrollPane = new javax.swing.JScrollPane();
        detectorDescription = new javax.swing.JEditorPane();
        jSeparator1 = new javax.swing.JSeparator();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        spacer = new javax.swing.JLabel();
        restoreDefaultsButton = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Configure Detectors");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        detectorTableScrollPane.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        detectorTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Bug Detector", "Speed", "Enabled"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        populateTable();
        detectorTable.getColumnModel().getColumn(ENABLED_COLUMN).setMaxWidth(60);
        detectorTable.getColumnModel().getColumn(SPEED_COLUMN).setMaxWidth(60);
        detectorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        {
            DefaultTableModel m = (DefaultTableModel)detectorTable.getModel();
            m.setColumnIdentifiers( new String[]
                {
                    L10N.getLocalString("dlg.bugdetector_lbl", "Bug Detector"),
                    L10N.getLocalString("dlg.speed_lbl", "Speed"),
                    L10N.getLocalString("dlg.enabled_lbl", "Enabled"),
                });

                DefaultSortedTableModel sortedModel = new DefaultSortedTableModel(m, detectorTable.getTableHeader());
                detectorTable.setModel(sortedModel);
            }

            detectorTableScrollPane.setViewportView(detectorTable);

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridwidth = 4;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.8;
            gridBagConstraints.insets = new java.awt.Insets(6, 6, 2, 6);
            getContentPane().add(detectorTableScrollPane, gridBagConstraints);

            detectorDescriptionScrollPane.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
            detectorDescriptionScrollPane.setPreferredSize(new java.awt.Dimension(110, 120));
            detectorDescriptionScrollPane.setViewportView(detectorDescription);

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.gridwidth = 4;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.weighty = 0.3;
            gridBagConstraints.insets = new java.awt.Insets(2, 6, 2, 6);
            getContentPane().add(detectorDescriptionScrollPane, gridBagConstraints);

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 4;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
            getContentPane().add(jSeparator1, gridBagConstraints);

            okButton.setMnemonic('O');
            okButton.setText("OK");
            okButton.setText(L10N.getLocalString("dlg.ok_btn","OK"));
            okButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    okButtonActionPerformed(evt);
                }
            });

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 2);
            getContentPane().add(okButton, gridBagConstraints);

            cancelButton.setMnemonic('C');
            cancelButton.setText("Cancel");
            cancelButton.setText(L10N.getLocalString("dlg.cancel_btn", "Cancel"));
            cancelButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    cancelButtonActionPerformed(evt);
                }
            });

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 4, 6);
            getContentPane().add(cancelButton, gridBagConstraints);

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            getContentPane().add(spacer, gridBagConstraints);

            restoreDefaultsButton.setText("Restore Defaults");
            restoreDefaultsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            restoreDefaultsButton.setText(L10N.getLocalString("dlg.restoredefaults_btn", "Restore Defaults"));
            restoreDefaultsButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    restoreDefaultsButtonActionPerformed(evt);
                }
            });

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 0);
            getContentPane().add(restoreDefaultsButton, gridBagConstraints);

            pack();
        }//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        setTitle(L10N.getLocalString("dlg.configuredetectors_ttl", "Configure Detectors"));
    }//GEN-LAST:event_formWindowOpened

	/**
	 * reverts the selected state of all the detectors to their defaults as specified in the findbugs.xml file
	 *
	 * @param evt the swing event corresponding to the mouse click of the Restore Defaults button
	 */
	private void restoreDefaultsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restoreDefaultsButtonActionPerformed
		Iterator<DetectorFactory> i = DetectorFactoryCollection.instance().factoryIterator();
		DefaultSortedTableModel sorter = (DefaultSortedTableModel) detectorTable.getModel();
		TableModel model = sorter.getBaseTableModel();
		int row = 0;
		while (i.hasNext()) {
			DetectorFactory factory = i.next();
			model.setValueAt(factory.isDefaultEnabled() ? Boolean.TRUE : Boolean.FALSE, row++, ENABLED_COLUMN);
		}
	}//GEN-LAST:event_restoreDefaultsButtonActionPerformed

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		closeDialog();
	}//GEN-LAST:event_cancelButtonActionPerformed

	private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
		// Update new enabled/disabled status for the Detectors
		int num = factoryList.size();
		DefaultSortedTableModel sorter = (DefaultSortedTableModel) detectorTable.getModel();
		TableModel model = sorter.getBaseTableModel();
		for (int i = 0; i < num; ++i) {
			DetectorFactory factory = factoryList.get(i);
			Boolean enabled = (Boolean) model.getValueAt(i, ENABLED_COLUMN);
			UserPreferences.getUserPreferences().enableDetector(
					factory, enabled.booleanValue());
		}
		closeDialog();
	}//GEN-LAST:event_okButtonActionPerformed

	/**
	 * Closes the dialog
	 */
	private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	/**
	 * installs a list selection listener to populate the bottom details page based on selection changes in top grid.
	 * A conversion from the table sorter index to the base model index is done to get the correct details
	 */
	private void postInitComponents() {
		// Listen to detector table selections so we can (hopefully)
		// display the description of the detector

		ListSelectionModel rowSM = detectorTable.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;

				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				if (!lsm.isSelectionEmpty()) {
					int selectedRow = lsm.getMinSelectionIndex();
					DefaultSortedTableModel sorter = (DefaultSortedTableModel) detectorTable.getModel();
					viewDetectorDetails(factoryList.get(sorter.getBaseModelIndex(selectedRow)));
				}
			}
		});
	}

	/**
	 * populates the bottom detector details pane based on the detector selected
	 *
	 * @param factory the detector that is currently selected
	 */
	private void viewDetectorDetails(DetectorFactory factory) {
		String detailHTML = factory.getDetailHTML();
		if (detailHTML == null) {
			detectorDescription.setText("");
		} else {
			detectorDescription.setContentType("text/html");
			detectorDescription.setText(detailHTML);
			StringBuffer toolTip = new StringBuffer(100);
			toolTip.append("<html><body><b>");
			toolTip.append(factory.getFullName());
			toolTip.append("</b><br><br><table border='1' width='100%'><tr><th>");
			toolTip.append(L10N.getLocalString("msg.bugpatternsreported_txt", "Bug Patterns Reported"));
			toolTip.append("</th></tr>");
			Collection<BugPattern> patterns = factory.getReportedBugPatterns();
			Iterator<BugPattern> it = patterns.iterator();
			while (it.hasNext()) {
				BugPattern pattern = it.next();
				toolTip.append("<tr><td align='center'>");
				toolTip.append(pattern.getType());
				toolTip.append("</td></tr>");
			}
			toolTip.append("</body></html>");
			detectorDescription.setToolTipText(toolTip.toString());
		}
	}

	/**
	 * populates the Detector JTable model with all available detectors
	 * Due to Netbeans form builder, populate table gets called before the tablesorter is installed, 
	 * so it is correct for the model retrieved from the table to be assumed to be the base DefaultTableModel.
	 */
	private void populateTable() {
		Iterator<DetectorFactory> i = DetectorFactoryCollection.instance().factoryIterator();
		while (i.hasNext()) {
			DetectorFactory factory = i.next();
			DefaultTableModel model = (DefaultTableModel) detectorTable.getModel();
			model.addRow(new Object[]{
					factory.getShortName(),
					factory.getSpeed(),
					UserPreferences.getUserPreferences().isDetectorEnabled(factory)
					});
			factoryList.add(factory);
		}
	}

	private void closeDialog() {
		setVisible(false);
		dispose();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		new ConfigureDetectorsDialog(new javax.swing.JFrame(), true).setVisible(true);
	}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JEditorPane detectorDescription;
    private javax.swing.JScrollPane detectorDescriptionScrollPane;
    private javax.swing.JTable detectorTable;
    private javax.swing.JScrollPane detectorTableScrollPane;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JButton okButton;
    private javax.swing.JButton restoreDefaultsButton;
    private javax.swing.JLabel spacer;
    // End of variables declaration//GEN-END:variables

	// My variables
	private ArrayList<DetectorFactory> factoryList = new ArrayList<DetectorFactory>();
}
