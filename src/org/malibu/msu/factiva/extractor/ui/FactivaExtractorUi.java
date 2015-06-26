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
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

import org.malibu.mail.Email;
import org.malibu.mail.EmailSender;
import org.malibu.msu.factiva.extractor.FactivaExtractorProgressToken;
import org.malibu.msu.factiva.extractor.FactivaExtractorThread;
import org.malibu.msu.factiva.extractor.FactivaKeywordValidatorThread;
import org.malibu.msu.factiva.extractor.beans.FactivaQuery;
import org.malibu.msu.factiva.extractor.exception.FactivaSpreadsheetException;
import org.malibu.msu.factiva.extractor.ss.FactivaQueryProgressCache;
import org.malibu.msu.factiva.extractor.ss.FactivaQuerySpreadsheetProcessor;
import org.malibu.msu.factiva.extractor.ui.listener.FactivaExtractorProgressListener;
import org.malibu.msu.factiva.extractor.ui.listener.JTextFieldChangeListener;
import org.malibu.msu.factiva.extractor.util.Constants;
import org.malibu.msu.factiva.extractor.util.FilesystemUtil;
import org.malibu.msu.factiva.extractor.util.StringUtil;
import org.malibu.msu.factiva.extractor.util.ValidationUtil;
import org.malibu.msu.factiva.extractor.web.FactivaWebHandlerConfig;

public class FactivaExtractorUi {

	private JFrame frmFactivaextractorV;
	
	private boolean resetVerifiedItemCache = false;
	private JTextField textField;
	
	private FactivaWebHandlerConfig config = new FactivaWebHandlerConfig();
	
	
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
		frmFactivaextractorV.setTitle("FactivaExtractor v2.1");
		frmFactivaextractorV.setBounds(100, 100, 650, 728);
		frmFactivaextractorV.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmFactivaextractorV.getContentPane().setLayout(null);
		
		final JFrame mainFrame = frmFactivaextractorV;
		
		final JProgressBar progressBar = new JProgressBar();
		final JLabel lblStatusMessage = new JLabel("Not started");
		
		JPanel panel = new JPanel();
		panel.setBounds(0, 0, 644, 695);
		panel.setBackground(Color.WHITE);
		frmFactivaextractorV.getContentPane().add(panel);
		panel.setLayout(null);

		addStep1Directions(panel);
		addUsernameLabelAndInput(panel);
		addPasswordLabelAndInput(panel);
		addSkipLoginCheckbox(panel);
		addLogo(panel);

		addStep2Directions(panel);
		addWorkingDirLabelAndButton(mainFrame, panel);

		addStep3Directions(panel);
		addValidateExcelSheetButton(panel);
		
		addStep4Directions(panel);
		addValidateExcelSheetDataButton(panel, progressBar, lblStatusMessage);
		
		addStep5Directions(panel);
		addEmailAddressLabelAndInput(panel);
		addSendTestEmailButton(panel);

		addStep6Directions(panel);
		addRunButton(progressBar, lblStatusMessage, panel);
		addProgressBar(progressBar, panel);
		addStatusLabel(lblStatusMessage, panel);
		addRecoverButton(panel);
		
		addLog(panel);
	}

	private void addStatusLabel(final JLabel lblStatusMessage, JPanel panel) {
		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStatus.setBounds(10, 448, 68, 16);
		panel.add(lblStatus);

		lblStatusMessage.setBounds(81, 448, 549, 16);
		panel.add(lblStatusMessage);
	}

	private void addProgressBar(final JProgressBar progressBar, JPanel panel) {
		JLabel lblStepCheck = new JLabel("Progress:");
		lblStepCheck.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepCheck.setBounds(10, 467, 68, 16);
		panel.add(lblStepCheck);

		progressBar.setBounds(81, 469, 549, 14);
		panel.add(progressBar);
	}

	private void addRunButton(final JProgressBar progressBar,
			final JLabel lblStatusMessage, JPanel panel) {
		JButton btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!ValidationUtil.verifyAll(config)) {
					return;
				}
				
				// get spreadsheet file path
				String workingDirPath = config.getWorkingDirPath();
				File workingDir = new File(workingDirPath);
				File[] filesInWorkingDir = workingDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File file, String fileName) {
						return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
					}
				});
				String spreadsheetFilePath = filesInWorkingDir[0].getAbsolutePath();
				
				// create output directory if it doesn't exist already
				String outputDirectoryPath = workingDirPath + Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.DESTINATION_DIRECTORY_NAME) + Constants.FILE_SEPARATOR;
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
				String tempDownloadDirPath = workingDirPath+ Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.TEMP_DOWNLOAD_DIRECTORY_NAME) + Constants.FILE_SEPARATOR;
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
					fireFoxProfileDirPath = FilesystemUtil.getJarDirectory() + Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.FIREFOX_PROFILE_DIR_NAME);
				}
				
				// set up progress token and events
				FactivaExtractorProgressToken progressToken = new FactivaExtractorProgressToken();
				progressToken.setListener(new FactivaExtractorProgressListener() {
					@Override
					public void stateChanged(FactivaExtractorProgressToken token) {
						progressBar.setValue(token.getPercentComplete());
						lblStatusMessage.setText(token.getStatusMessage());
						if(token.getStatusMessage() != null && token.getStatusMessage().trim().length() != 0) {
							MessageHandler.logMessage("(" + token.getPercentComplete() + "%) - " + token.getStatusMessage());
						}
					}
				});
				
				config.setSpreadsheetFilePath(spreadsheetFilePath);
				config.setTempDownloadDirPath(tempDownloadDirPath);
				config.setDestinationDirPath(outputDirectoryPath);
				config.setFirefoxProfileDirPath(fireFoxProfileDirPath);
				config.setProgressToken(progressToken);
				
				new Thread(new FactivaExtractorThread(config)).start();
			}
		});
		btnRun.setBounds(10, 410, 97, 25);
		panel.add(btnRun);
	}

	private void addSkipLoginCheckbox(JPanel panel) {
		JCheckBox chckbxSkipLogin = new JCheckBox("Skip login");
		chckbxSkipLogin.setBackground(Color.WHITE);
		chckbxSkipLogin.setBounds(487, 117, 113, 25);
		chckbxSkipLogin.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				config.setSkipLogin(((AbstractButton)e.getSource()).isSelected());
			}
		});
		panel.add(chckbxSkipLogin);
	}

	private void addSendTestEmailButton(JPanel panel) {
		JButton btnSendTestEmail = new JButton("Send Test Email");
		btnSendTestEmail.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String alertEmailAddress = textField.getText();
				if(alertEmailAddress != null && alertEmailAddress.trim().length() != 0) {
					MessageHandler.logMessage("Sending test email to: " + alertEmailAddress);
					Email email = new Email();
					email.setToAddress(alertEmailAddress);
					email.setSubject("FactivaExtractor TEST Email");
					email.setMessage("Test verification message sent at: " + new Date());
					MessageHandler.logMessage("email message successfully sent");
					try {
						EmailSender.sendEmail(email);
					} catch (Exception e) {
						MessageHandler.handleException("Failed to send test email", e);
					}
				}
			}
		});
		btnSendTestEmail.setBounds(260, 358, 158, 25);
		panel.add(btnSendTestEmail);
	}

	private void addRecoverButton(JPanel panel) {
		JButton btnUpdateSpreadsheetFrom = new JButton("Try to Recover from Failed Run");
		btnUpdateSpreadsheetFrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(!ValidationUtil.verifyAll(config)) {
					return;
				}
				
				File workingDir = new File(config.getWorkingDirPath());
				File[] filesInWorkingDir = workingDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File file, String fileName) {
						return fileName != null && (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx"));
					}
				});
				String spreadsheetFilePath = filesInWorkingDir[0].getAbsolutePath();
				
				// load cache and spreadsheet
				FactivaQuerySpreadsheetProcessor spreadsheet = null;
				FactivaQueryProgressCache progressCache = null;
				try {
					progressCache = new FactivaQueryProgressCache(workingDir.getAbsolutePath());
					// let user know if cache doesn't exist
					if(!progressCache.doesCacheFileExist()) {
						MessageHandler.showMessage("No cache to recover from, recovery not possible");
						return;
					}
					progressCache.close(); // we don't want to write to it, just read
					spreadsheet = new FactivaQuerySpreadsheetProcessor(spreadsheetFilePath);
				} catch (IOException | FactivaSpreadsheetException e) {
					MessageHandler.handleException("Failed to load input spreadsheet and cache", e);
					return;
				}
				
				// write progress cache data to Excel file
				MessageHandler.logMessage("Writing progress cache to Excel file...");
				try {
					progressCache.writeCachedEntriesToSpreadsheet(spreadsheet);
				} catch (Exception e) {
					MessageHandler.handleException("Failed to write to progress cache to Excel file, cache may be corrupted.  Exiting...", e);
					return;
				}
				try {
					spreadsheet.saveWorkbook();
				} catch (Exception e) {
					MessageHandler.handleException("Failed to save updated Excel file", e);
					return;
				}
				// attempt to delete cache
				MessageHandler.logMessage("Deleting cache...");
				try {
					progressCache.deleteCache();
				} catch (Exception e) {
					MessageHandler.logMessage("Failed to delete cache file, may need to be deleted manually");
				}
				MessageHandler.showMessage("Recovery successful!");
				MessageHandler.logMessage("Finished updating spreadsheet from cache");
			}
		});
		btnUpdateSpreadsheetFrom.setBounds(124, 410, 260, 25);
		panel.add(btnUpdateSpreadsheetFrom);
	}

	private void addStep4Directions(JPanel panel) {
		JLabel lblStepoptional = new JLabel("Step 4 (optional): Validate spreadsheet Source, Company, and Subjects");
		lblStepoptional.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepoptional.setBounds(10, 283, 587, 16);
		panel.add(lblStepoptional);
	}

	private void addValidateExcelSheetDataButton(JPanel panel,
			final JProgressBar progressBar, final JLabel lblStatusMessage) {
		JButton btnValidate_1 = new JButton("Validate");
		btnValidate_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(!ValidationUtil.verifyAll(config)) {
					return;
				}
				
				// get spreadsheet file path
				File workingDir = new File(config.getWorkingDirPath());
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
					fireFoxProfileDirPath = FilesystemUtil.getJarDirectory() + Constants.FILE_SEPARATOR + Constants.getInstance().getConstant(Constants.FIREFOX_PROFILE_DIR_NAME);
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
				
				config.setSpreadsheetFilePath(spreadsheetFilePath);
				config.setFirefoxProfileDirPath(fireFoxProfileDirPath);
				config.setProgressToken(progressToken);
				
				new Thread(new FactivaKeywordValidatorThread(config, resetVerifiedItemCache)).start();
				resetVerifiedItemCache = false;
			}
		});
		btnValidate_1.setBounds(10, 306, 97, 25);
		panel.add(btnValidate_1);
	}

	private void addEmailAddressLabelAndInput(JPanel panel) {
		JLabel lblEmailAddress = new JLabel("Email address:");
		lblEmailAddress.setBounds(10, 362, 97, 16);
		panel.add(lblEmailAddress);
		
		textField = new JTextField();
		textField.setBounds(106, 359, 146, 22);
		textField.getDocument().addDocumentListener(new JTextFieldChangeListener(textField) {
			public void onChange(String newText) {
				if(StringUtil.isEmpty(newText)) {
					config.setAlertEmailAddress(null);
				} else {
					config.setAlertEmailAddress(newText.trim());
				}
			}
		});
		panel.add(textField);
		textField.setColumns(10);
	}

	private void addStep5Directions(JPanel panel) {
		JLabel lblStepoptional_1 = new JLabel("Step 5 (optional): Specify an email address to contact if processing has errors or completes");
		lblStepoptional_1.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepoptional_1.setBounds(10, 339, 620, 16);
		panel.add(lblStepoptional_1);
	}

	private void addLog(JPanel panel) {
		JLabel lblLog_1 = new JLabel("Log:");
		lblLog_1.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblLog_1.setBounds(10, 496, 33, 16);
		panel.add(lblLog_1);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBounds(43, 496, 587, 186);
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

	private void addStep6Directions(JPanel panel) {
		JLabel lblStepRun = new JLabel(
				"Step 6: Run (will display status messages below)");
		lblStepRun.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepRun.setBounds(10, 391, 325, 16);
		panel.add(lblStepRun);
	}

	private void addValidateExcelSheetButton(JPanel panel) {
		JButton btnValidate = new JButton("Validate");
		btnValidate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!ValidationUtil.verifyWorkingDirectory(config)) {
					return;
				}
				File workingDirectoryFile = new File(config.getWorkingDirPath());
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
				config.setSpreadsheetVerified(true);
			}
		});
		btnValidate.setBounds(10, 245, 97, 25);
		panel.add(btnValidate);
	}

	private void addStep3Directions(JPanel panel) {
		JLabel lblStepValidate = new JLabel(
				"Step 3: Validate the Excel file (make sure no issues exist)");
		lblStepValidate.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepValidate.setBounds(10, 227, 620, 16);
		panel.add(lblStepValidate);
	}

	private void addWorkingDirLabelAndButton(final JFrame mainFrame,
			JPanel panel) {
		final JLabel lblDirectory = new JLabel("<please select a directory>");
		lblDirectory.setBounds(193, 198, 437, 16);
		panel.add(lblDirectory);

		JButton btnSelectWorkingDirectory = new JButton(
				"Select working directory");
		btnSelectWorkingDirectory.setBounds(10, 194, 171, 25);
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
			    	config.setWorkingDirPath(chooser.getSelectedFile().getAbsolutePath());
			    } else {
			    	lblDirectory.setText("<please select a directory>");
			    	config.setWorkingDirPath("");
			    }
			    config.setSpreadsheetVerified(false);
			    resetVerifiedItemCache = true;
			}
		});
		panel.add(btnSelectWorkingDirectory);
	}

	private void addStep1Directions(JPanel panel) {
		JLabel lblFactivaLoginCredentials = new JLabel(
				"Step 1: Factiva Login Credentials");
		lblFactivaLoginCredentials.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblFactivaLoginCredentials.setBounds(10, 121, 238, 16);
		panel.add(lblFactivaLoginCredentials);
	}

	private void addStep2Directions(JPanel panel) {
		JLabel lblStepSelect = new JLabel(
				"Step 2: Select working directory (should be an empty directory except for your excel file)");
		lblStepSelect.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblStepSelect.setBounds(10, 173, 620, 16);
		panel.add(lblStepSelect);
	}

	private void addLogo(JPanel panel) throws IOException {
		BufferedImage myPicture = ImageIO.read(FactivaExtractorUi.class
				.getClassLoader().getResourceAsStream("logo.jpg"));
		JLabel picLabel = new JLabel(new ImageIcon(myPicture));
		picLabel.setBounds(0, 0, 650, 120);
		panel.add(picLabel);
	}

	private void addPasswordLabelAndInput(JPanel panel) {
		// password
		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setBounds(260, 145, 60, 16);
		panel.add(lblPassword);

		JPasswordField textFieldPassword = new JPasswordField(System.getProperty("PASSWORD"));
		textFieldPassword.setBounds(328, 142, 151, 22);
		textFieldPassword.getDocument().addDocumentListener(new JTextFieldChangeListener(textFieldPassword) {
			public void onChange(String newText) {
				config.setPassword(newText);
			}
		});
		panel.add(textFieldPassword);
		textFieldPassword.setColumns(10);
	}

	private void addUsernameLabelAndInput(JPanel panel) {
		// username
		JLabel lblUsername = new JLabel("Username:");
		lblUsername.setBounds(260, 121, 63, 16);
		panel.add(lblUsername);

		JTextField textFieldUsername = new JTextField(System.getProperty("USERNAME"));
		textFieldUsername.setBounds(328, 118, 151, 22);
		textFieldUsername.getDocument().addDocumentListener(new JTextFieldChangeListener(textFieldUsername) {
			public void onChange(String newText) {
				config.setUsername(newText);
			}
		});
		panel.add(textFieldUsername);
		textFieldUsername.setColumns(10);
	}
}
