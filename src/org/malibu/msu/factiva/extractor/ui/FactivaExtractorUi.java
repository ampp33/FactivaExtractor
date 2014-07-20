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
import org.malibu.msu.factiva.extractor.FactivaKeywordValidatorThread;
import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.util.Constants;
import org.malibu.msu.factiva.extractor.util.FilesystemUtil;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class FactivaExtractorUi {

	private JFrame frmFactivaextractorV;
	private JLabel lblDirectory;
	private JTextField textFieldUsername;
	private JTextField textFieldPassword;
	
	private boolean spreadsheetVerified = false;
	private JTextField textField;
	
	
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
		frmFactivaextractorV.setBounds(100, 100, 650, 681);
		frmFactivaextractorV.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmFactivaextractorV.getContentPane().setLayout(null);
		
		final JFrame mainFrame = frmFactivaextractorV;
		
		JPanel panel = new JPanel();
		panel.setBounds(0, 0, 644, 648);
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
				"Step 6: Run (will display status messages below)");
		lblStepRun.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepRun.setBounds(10, 368, 325, 16);
		panel.add(lblStepRun);
		
		final JProgressBar progressBar = new JProgressBar();
		final JLabel lblStatusMessage = new JLabel("Not started");
		
		JButton btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!verifyAll()) {
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
				
				// create output directory if it doesn't exist already
				String outputDirectoryPath = lblDirectory.getText() + Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.DESTINATION_DIRECTORY_NAME) + Constants.FILE_SEPARATOR;
				File outputDir = new File(outputDirectoryPath);
				if(!(outputDir.exists() && outputDir.isDirectory())) {
					if(!outputDir.mkdir()) {
						String errorMessage = "error occurred creating output directory: '" + outputDirectoryPath + "'";
						MessageHandler.showErrorMessage(errorMessage);
						MessageHandler.logMessage(errorMessage);
						return;
					}
				}
				
				// create temp download directory if it doesn't exist already
				String tempDownloadDirPath = lblDirectory.getText() + Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.TEMP_DOWNLOAD_DIRECTORY_NAME) + Constants.FILE_SEPARATOR;
				File tempDownloadDir = new File(tempDownloadDirPath);
				if(!(tempDownloadDir.exists() && tempDownloadDir.isDirectory())) {
					if(!tempDownloadDir.mkdir()) {
						String errorMessage = "error occurred creating temp download directory: '" + tempDownloadDirPath + "'";
						MessageHandler.showErrorMessage(errorMessage);
						MessageHandler.logMessage(errorMessage);
						return;
					}
				}
				// check if temp download directory has files in it already, and if it does, alert the user
				File[] tempDirFiles = tempDownloadDir.listFiles();
				if(tempDirFiles != null && tempDirFiles.length > 0) {
					String errorMessage = "temp download directory: '" + tempDownloadDirPath + "' is not empty, please empty this directory";
					MessageHandler.showErrorMessage(errorMessage);
					MessageHandler.logMessage(errorMessage);
					return;
				}
				
				// get firefox profile dir location, depending on whether or not it's location is overridden
				String fireFoxProfileDirPath = null;
				if(System.getProperty(Constants.OVERRIDE_FIREFOX_PROFILE_DIR) != null) {
					fireFoxProfileDirPath = System.getProperty(Constants.OVERRIDE_FIREFOX_PROFILE_DIR) + Constants.getInstance().getConstant(Constants.FIREFOX_PROFILE_DIR_NAME);
				} else {
					fireFoxProfileDirPath = FilesystemUtil.getJarDirectory() + Constants.FIREFOX_PROFILE_DIR_NAME;
				}
				
				FactivaExtractorProgressToken progressToken = new FactivaExtractorProgressToken();
				progressToken.setListener(new FactivaExtractorProgressListener() {
					@Override
					public void stateChanged(FactivaExtractorProgressToken token) {
						progressBar.setValue(token.getPercentComplete());
						lblStatusMessage.setText(token.getStatusMessage());
						MessageHandler.logMessage("(" + token.getPercentComplete() + "%) - " + token.getStatusMessage());
					}
				});
				
				FactivaWebHandlerConfig config = new FactivaWebHandlerConfig();
				config.setUsername(username);
				config.setPassword(password);
				config.setWorkingDirPath(workingDir.getAbsolutePath());
				config.setSpreadsheetFilePath(spreadsheetFilePath);
				config.setTempDownloadDirPath(tempDownloadDirPath);
				config.setDestinationDirPath(outputDirectoryPath);
				config.setFirefoxProfileDirPath(fireFoxProfileDirPath);
				config.setProgressToken(progressToken);
				
				new Thread(new FactivaExtractorThread(config)).start();
			}
		});
		btnRun.setBounds(10, 387, 97, 25);
		panel.add(btnRun);

		JLabel lblStepCheck = new JLabel("Progress:");
		lblStepCheck.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepCheck.setBounds(10, 444, 68, 16);
		panel.add(lblStepCheck);

		progressBar.setBounds(81, 446, 549, 14);
		panel.add(progressBar);

		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStatus.setBounds(10, 425, 68, 16);
		panel.add(lblStatus);

		lblStatusMessage.setBounds(81, 425, 549, 16);
		panel.add(lblStatusMessage);

		JLabel lblLog_1 = new JLabel("Log:");
		lblLog_1.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblLog_1.setBounds(10, 473, 33, 16);
		panel.add(lblLog_1);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBounds(43, 473, 587, 162);
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
		
		JLabel lblStepoptional = new JLabel("Step 4 (optional): Validate spreadsheet Source, Company, and Subjects");
		lblStepoptional.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepoptional.setBounds(10, 260, 587, 16);
		panel.add(lblStepoptional);
		
		JButton btnValidate_1 = new JButton("Validate");
		btnValidate_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(!verifyAll()) {
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
				
				
				// get firefox profile dir location, depending on whether or not it's location is overridden
				String fireFoxProfileDirPath = null;
				if(System.getProperty(Constants.OVERRIDE_FIREFOX_PROFILE_DIR) != null) {
					fireFoxProfileDirPath = System.getProperty(Constants.OVERRIDE_FIREFOX_PROFILE_DIR) + Constants.getInstance().getConstant(Constants.FIREFOX_PROFILE_DIR_NAME);
				} else {
					fireFoxProfileDirPath = FilesystemUtil.getJarDirectory() + Constants.FIREFOX_PROFILE_DIR_NAME;
				}
				
				FactivaExtractorProgressToken progressToken = new FactivaExtractorProgressToken();
				progressToken.setListener(new FactivaExtractorProgressListener() {
					@Override
					public void stateChanged(FactivaExtractorProgressToken token) {
						progressBar.setValue(token.getPercentComplete());
						lblStatusMessage.setText(token.getStatusMessage());
						MessageHandler.logMessage("(" + token.getPercentComplete() + "%) - " + token.getStatusMessage());
					}
				});
				
				FactivaWebHandlerConfig config = new FactivaWebHandlerConfig();
				config.setUsername(username);
				config.setPassword(password);
				config.setSpreadsheetFilePath(spreadsheetFilePath);
				config.setFirefoxProfileDirPath(fireFoxProfileDirPath);
				config.setProgressToken(progressToken);
				
				new Thread(new FactivaKeywordValidatorThread(config)).start();
			}
		});
		btnValidate_1.setBounds(10, 283, 97, 25);
		panel.add(btnValidate_1);
		
		JLabel lblStepoptional_1 = new JLabel("Step 5 (optional): Specify an email address to email an alert if this monitor runs into errors");
		lblStepoptional_1.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepoptional_1.setBounds(10, 316, 604, 16);
		panel.add(lblStepoptional_1);
		
		JLabel lblEmailAddress = new JLabel("Email address:");
		lblEmailAddress.setBounds(10, 339, 97, 16);
		panel.add(lblEmailAddress);
		
		textField = new JTextField();
		textField.setBounds(106, 336, 217, 22);
		panel.add(textField);
		textField.setColumns(10);
	}
	
	private boolean verifyWorkingDirectory() {
		// verify working directory is set and exists
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
		// verify Firefox profile directory exists
//		String firefoxProfileDirPath = workingDirectoryFile.getAbsolutePath() + Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.FIREFOX_PROFILE_DIR_NAME);
//		File firefoxProfileDir = new File(firefoxProfileDirPath);
//		if(!firefoxProfileDir.exists() || firefoxProfileDir.isDirectory()) {
//			MessageHandler.showErrorMessage("Firefox profile directory not found in working directory");
//			return false;
//		}
		// verify only one Excel file exists in the working directory
		File[] filesInWorkingDir = workingDirectoryFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String fileName) {
				return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
			}
		});
		if(filesInWorkingDir.length == 0) {
			MessageHandler.showErrorMessage("No Excel file found in working directory");
			return false;
		}
		if(filesInWorkingDir.length > 1) {
			MessageHandler.showErrorMessage("More than one Excel file found in working directory, or file is open in Excel!");
			return false;
		}
		return true;
	}
	
	private boolean verifyAll() {
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
