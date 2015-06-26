package org.malibu.msu.factiva.extractor.ui.listener;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class JTextFieldChangeListener implements DocumentListener {
	
	private JTextField textField;
	
	public JTextFieldChangeListener(JTextField textField) {
		this.textField = textField;
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		event();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		event();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		event();
	}
	
	private void event() {
		onChange(textField.getText());
	}
	
	public abstract void onChange(String newText);
	
}
