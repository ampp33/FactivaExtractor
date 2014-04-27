package org.malibu.msu.factiva.extractor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
import javax.swing.text.DefaultCaret;

import org.malibu.msu.factiva.extractor.FactivaExtractorProgressListener;
import org.malibu.msu.factiva.extractor.FactivaExtractorProgressToken;
import org.malibu.msu.factiva.extractor.FactivaExtractorThread;
import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.util.Constants;

public class FactivaExtractorUi {

	private JFrame frmFactivaextractorV;
	private JLabel lblDirectory;
	private JTextField textFieldUsername;
	private JTextField textFieldPassword;
	
	private boolean spreadsheetVerified = false;
	
	
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
		frmFactivaextractorV.setBounds(100, 100, 650, 500);
		frmFactivaextractorV.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmFactivaextractorV.getContentPane().setLayout(null);
		
		final JFrame mainFrame = frmFactivaextractorV;
		
		JPanel panel = new JPanel();
		panel.setBounds(0, 0, 644, 469);
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

		textFieldUsername = new JTextField(System.getProperty("USERNAME"));
		textFieldUsername.setBounds(328, 118, 116, 22);
		panel.add(textFieldUsername);
		textFieldUsername.setColumns(10);

		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setBounds(449, 121, 60, 16);
		panel.add(lblPassword);

		textFieldPassword = new JPasswordField(System.getProperty("PASSWORD"));
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
		
		lblDirectory = new JLabel("<please select a directory>");
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
			    spreadsheetVerified = false;
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
				if(!verifyWorkingDirectory()) {
					return;
				}
				File workingDirectoryFile = new File(lblDirectory.getText());
				File[] filesInWorkingDir = workingDirectoryFile.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File file, String fileName) {
						return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
					}
				});
				String excelFilePath = filesInWorkingDir[0].getAbsolutePath();
				try {
					FactivaQuerySpreadsheetProcessor processor = new FactivaQuerySpreadsheetProcessor(excelFilePath);
					List<FactivaQuery> queries = processor.getQueriesFromSpreadsheet(false);
					List<String> errorMessages = processor.validateQueries(queries, false);
					if(errorMessages.size() > 0) {
						MessageHandler.showMultipleErrorMessages("errors detected in Excel file:", errorMessages);
						return;
					}
				} catch (IOException | FactivaSpreadsheetException e1) {
					MessageHandler.handleException("failed to verify Excel file", e1);
					return;
				}
				MessageHandler.logMessage("Excel file '" + excelFilePath + "' validated successfully!");
				MessageHandler.showMessage("Excel file '" + excelFilePath + "' validated successfully!");
				spreadsheetVerified = true;
			}
		});
		btnValidate.setBounds(10, 222, 97, 25);
		panel.add(btnValidate);

		JLabel lblStepRun = new JLabel(
				"Step 4: Run (will display status messages below)");
		lblStepRun.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepRun.setBounds(10, 253, 325, 16);
		panel.add(lblStepRun);
		
		final JProgressBar progressBar = new JProgressBar();
		final JLabel lblStatusMessage = new JLabel("Not started");
		
		JButton btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!verifyAllFields()) {
					return;
				}
				
				// get username and password
				String username = textFieldUsername.getText();
				String password = textFieldPassword.getText();
				
				// get spreadsheet file path
				File workingDir = new File(lblDirectory.getText());
				File[] filesInWorkingDir = workingDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File file, String fileName) {
						return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
					}
				});
				String spreadsheetFilePath = filesInWorkingDir[0].getAbsolutePath();
				
				// TODO: add validation if these don't work
				// create output directory if it doesn't exist already
				String outputDirectoryPath = lblDirectory.getText() + "\\" + Constants.getInstance().getConstant(Constants.DESTINATION_DIRECTORY_NAME) + "\\";
				File outputDir = new File(outputDirectoryPath);
				if(!(outputDir.exists() && outputDir.isDirectory())) {
					outputDir.mkdir();
				}
				
				// create temp download directory if it doesn't exist already
				String tempDownloadDirPath = lblDirectory.getText() + "\\" + Constants.getInstance().getConstant(Constants.TEMP_DOWNLOAD_DIRECTORY_NAME) + "\\";
				File tempDownloadDir = new File(tempDownloadDirPath);
				if(!(tempDownloadDir.exists() && tempDownloadDir.isDirectory())) {
					tempDownloadDir.mkdir();
				}
				// TODO: delete all files in dirs if any exist
				
				FactivaExtractorProgressToken progressToken = new FactivaExtractorProgressToken();
				progressToken.setListener(new FactivaExtractorProgressListener() {
					@Override
					public void stateChanged(FactivaExtractorProgressToken token) {
						progressBar.setValue(token.getPercentComplete());
						lblStatusMessage.setText(token.getStatusMessage());
						MessageHandler.logMessage("(" + token.getPercentComplete() + "%) - " + token.getStatusMessage());
					}
				});
				new Thread(new FactivaExtractorThread(username, password, spreadsheetFilePath, tempDownloadDirPath, outputDirectoryPath, progressToken)).start();
			}
		});
		btnRun.setBounds(10, 272, 97, 25);
		panel.add(btnRun);

		JLabel lblStepCheck = new JLabel("Progress:");
		lblStepCheck.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepCheck.setBounds(10, 325, 68, 16);
		panel.add(lblStepCheck);

		progressBar.setBounds(81, 327, 549, 14);
		panel.add(progressBar);

		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStatus.setBounds(10, 306, 68, 16);
		panel.add(lblStatus);

		lblStatusMessage.setBounds(81, 306, 549, 16);
		panel.add(lblStatusMessage);

		JLabel lblLog_1 = new JLabel("Log:");
		lblLog_1.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblLog_1.setBounds(10, 350, 33, 16);
		panel.add(lblLog_1);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBounds(45, 351, 587, 103);
		panel.add(panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));
				
		JTextArea logTextArea = new JTextArea(0, 0);
		logTextArea.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
		DefaultCaret caret = (DefaultCaret)logTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
//		logTextArea.setEditable(false); // set textArea non-editable
		JScrollPane scroll = new JScrollPane(logTextArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panel_1.add(scroll, BorderLayout.CENTER);
		
		MessageHandler.setLogTextArea(logTextArea);
	}
	
	private boolean verifyWorkingDirectory() {
		String workingDirectory = lblDirectory.getText();
		if(workingDirectory == null || workingDirectory.trim().length() == 0) {
			MessageHandler.showErrorMessage("no working directory chosen");
			return false;
		}
		File workingDirectoryFile = new File(workingDirectory);
		if(!workingDirectoryFile.exists() || !workingDirectoryFile.isDirectory()) {
			MessageHandler.showErrorMessage("working directory invalid, or not a directory");
			return false;
		}
		File[] filesInWorkingDir = workingDirectoryFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String fileName) {
				return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
			}
		});
		if(filesInWorkingDir.length == 0) {
			MessageHandler.showErrorMessage("no Excel file found in working directory");
			return false;
		}
		if(filesInWorkingDir.length > 1) {
			MessageHandler.showErrorMessage("more than one Excel file found in working directory!");
			return false;
		}
		return true;
	}
	
	private boolean verifyAllFields() {
		if(!verifyWorkingDirectory()) {
			return false;
		}
		if(textFieldUsername.getText() == null || textFieldUsername.getText().trim().length() == 0) {
			MessageHandler.showErrorMessage("No username specified");
			return false;
		}
		if(textFieldPassword.getText() == null || textFieldPassword.getText().trim().length() == 0) {
			MessageHandler.showErrorMessage("No password specified");
			return false;
		}
		if(!spreadsheetVerified) {
			MessageHandler.showErrorMessage("Spreadsheet not yet been successfully verified, please verify first");
			return false;
		}
		return true;
	}
}
