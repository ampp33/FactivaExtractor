package org.malibu.msu.factiva.extractor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;

public class FactivaExtractorUi {

	private JFrame frmFactivaextractorV;
	private JTextField textFieldUsername;
	private JTextField textFieldPassword;
	
	private String outputDirectory = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Throwable t) {}
				try {
					FactivaExtractorUi window = new FactivaExtractorUi();
					window.frmFactivaextractorV.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws IOException
	 */
	public FactivaExtractorUi() throws IOException {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 * 
	 * @throws IOException
	 */
	private void initialize() throws IOException {
		frmFactivaextractorV = new JFrame();
		frmFactivaextractorV.setResizable(false);
		frmFactivaextractorV.setTitle("FactivaExtractor v1.0");
		frmFactivaextractorV.setBounds(100, 100, 650, 515);
		frmFactivaextractorV.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmFactivaextractorV.getContentPane().setLayout(null);
		
		final JFrame mainFrame = frmFactivaextractorV;
		
		JPanel panel = new JPanel();
		panel.setBounds(0, 0, 644, 482);
		panel.setBackground(Color.WHITE);
		frmFactivaextractorV.getContentPane().add(panel);
		panel.setLayout(null);

		JLabel lblFactivaLoginCredentials = new JLabel(
				"Step 1: Factiva Login Credentials");
		lblFactivaLoginCredentials.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblFactivaLoginCredentials.setBounds(10, 121, 238, 16);
		panel.add(lblFactivaLoginCredentials);

		JLabel lblUsername = new JLabel("Username:");
		lblUsername.setBounds(260, 121, 63, 16);
		panel.add(lblUsername);

		textFieldUsername = new JTextField();
		textFieldUsername.setBounds(328, 118, 116, 22);
		panel.add(textFieldUsername);
		textFieldUsername.setColumns(10);

		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setBounds(449, 121, 60, 16);
		panel.add(lblPassword);

		textFieldPassword = new JPasswordField();
		textFieldPassword.setBounds(514, 118, 116, 22);
		panel.add(textFieldPassword);
		textFieldPassword.setColumns(10);

		BufferedImage myPicture = ImageIO.read(FactivaExtractorUi.class
				.getClassLoader().getResourceAsStream("logo.jpg"));
		JLabel picLabel = new JLabel(new ImageIcon(myPicture));
		picLabel.setBounds(0, 0, 650, 120);
		panel.add(picLabel);

		JLabel lblStepSelect = new JLabel(
				"Step 2: Select working directory (should be an empty directory except for your excel file)");
		lblStepSelect.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepSelect.setBounds(10, 150, 620, 16);
		panel.add(lblStepSelect);
		
		final JLabel lblDirectory = new JLabel("<please select a directory>");
		lblDirectory.setBounds(193, 175, 437, 16);
		panel.add(lblDirectory);

		JButton btnSelectWorkingDirectory = new JButton(
				"Select working directory");
		btnSelectWorkingDirectory.setBounds(10, 171, 171, 25);
		btnSelectWorkingDirectory.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser(); 
			    chooser.setCurrentDirectory(new java.io.File("."));
			    chooser.setDialogTitle("Choose output directory");
			    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			    // disable the "All files" option.
			    chooser.setAcceptAllFileFilterUsed(false);
			    if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
			    	lblDirectory.setText(chooser.getSelectedFile().getAbsolutePath());
			    } else {
			    	lblDirectory.setText("<please select a directory>");
			    }
			}
		});
		panel.add(btnSelectWorkingDirectory);

		JLabel lblStepValidate = new JLabel(
				"Step 3: Validate the Excel file (make sure no issues exist)");
		lblStepValidate.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepValidate.setBounds(10, 204, 620, 16);
		panel.add(lblStepValidate);

		JButton btnValidate = new JButton("Validate");
		btnValidate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String workingDirectory = lblDirectory.getText();
				if(workingDirectory == null || workingDirectory.trim().length() == 0) {
					MessageHandler.showErrorMessage("no working directory chosen");
					return;
				}
				File workingDirectoryFile = new File(workingDirectory);
				if(!workingDirectoryFile.exists() || !workingDirectoryFile.isDirectory()) {
					MessageHandler.showErrorMessage("working directory invalid, or not a directory");
					return;
				}
				File[] filesInWorkingDir = workingDirectoryFile.listFiles();
				String excelFilePath = null;
				int numExcelFilesFound = 0;
				for (File file : filesInWorkingDir) {
					if(file.getName().toLowerCase().endsWith(".xls") || file.getName().toLowerCase().endsWith(".xlsx")) {
						excelFilePath = file.getAbsolutePath();
						numExcelFilesFound++;
					}
				}
				if(numExcelFilesFound == 0) {
					MessageHandler.showErrorMessage("no Excel file found in working directory");
					return;
				}
				if(numExcelFilesFound > 1) {
					MessageHandler.showErrorMessage("more than one Excel file found in working directory!");
					return;
				}
				try {
					FactivaQuerySpreadsheetProcessor processor = new FactivaQuerySpreadsheetProcessor(excelFilePath);
					List<FactivaQuery> queries = processor.getQueriesFromSpreadsheet(false);
					List<String> errorMessages = new ArrayList<>();
					for (FactivaQuery factivaQuery : queries) {
						errorMessages.addAll(processor.validateQuery(factivaQuery, false));
					}
					if(errorMessages.size() > 0) {
						MessageHandler.showMultipleErrorMessages("errors detected in Excel file:", errorMessages);
						return;
					}
				} catch (IOException | FactivaSpreadsheetException e1) {
					MessageHandler.handleException("failed to verify Excel file", e1);
					return;
				}
				MessageHandler.showMessage("Excel file validated successfully!");
				MessageHandler.logMessage("Excel file validated successfully!");
			}
		});
		btnValidate.setBounds(10, 222, 97, 25);
		panel.add(btnValidate);

		JLabel lblStepRun = new JLabel(
				"Step 4: Run (will display status messages below)");
		lblStepRun.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepRun.setBounds(10, 253, 325, 16);
		panel.add(lblStepRun);

		JButton btnRun = new JButton("Run");
		btnRun.setBounds(10, 272, 97, 25);
		panel.add(btnRun);

		JLabel lblStepCheck = new JLabel("Progress:");
		lblStepCheck.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepCheck.setBounds(10, 325, 68, 16);
		panel.add(lblStepCheck);

		JProgressBar progressBar = new JProgressBar();
		progressBar.setBounds(81, 327, 549, 14);
		panel.add(progressBar);

		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStatus.setBounds(10, 306, 68, 16);
		panel.add(lblStatus);

		JLabel lblNotStarted = new JLabel("Not started");
		lblNotStarted.setBounds(81, 306, 549, 16);
		panel.add(lblNotStarted);

		JLabel lblLog = new JLabel("Current Item:");
		lblLog.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblLog.setBounds(10, 345, 97, 16);
		panel.add(lblLog);

		JLabel label = new JLabel("<item id>");
		label.setBounds(108, 345, 522, 16);
		panel.add(label);

		JLabel lblLog_1 = new JLabel("Log:");
		lblLog_1.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblLog_1.setBounds(10, 366, 97, 16);
		panel.add(lblLog_1);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBounds(43, 366, 587, 103);
		panel.add(panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));
				
		JTextArea logTextArea = new JTextArea(0, 0);
		logTextArea.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
//		logTextArea.setEditable(false); // set textArea non-editable
		JScrollPane scroll = new JScrollPane(logTextArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panel_1.add(scroll, BorderLayout.CENTER);
		
		MessageHandler.setLogTextArea(logTextArea);
	}
}
