package presentation.gui.menu;

import logging.Log;
import presentation.controllers.MainController;
import presentation.gui.choosers.SchematicChooser;
import presentation.main.Constants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FileMenu extends JMenu {
	private static final long serialVersionUID = 9058054431044220813L;

	private MainController controller;
	
	public FileMenu(MainController controller) {
		super("File");
		
		this.controller = controller;
		
		buildGUI();
	}
	
	public void buildGUI() {
		
		setMnemonic('F');
		
		JMenuItem newItem		= new JMenuItem("New");
		JMenuItem openItem		= new JMenuItem("Open...");
		JMenuItem saveItem		= new JMenuItem("Save All");
		newItem					.setMnemonic('N');
		openItem				.setMnemonic('O');
		saveItem				.setMnemonic('S');
		add(newItem);
		add(openItem);
		add(saveItem);
		
		add(new JSeparator());
		
		JMenuItem exportItem	= new JMenuItem("Export...");
		exportItem				.setMnemonic('E');
		add(exportItem);
		
		add(new JSeparator());

		JMenuItem exitItem		= new JMenuItem("Exit");
		exitItem				.setMnemonic('x');
		add(exitItem);
		
		newItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.openNewWorldDialog();
			}
		});
		
		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				SchematicChooser chooser = new SchematicChooser(new File(Constants.SCHEMATICSDIR));
				int result =  chooser.showOpenDialog(controller.getRSframe());
				
				if (result != SchematicChooser.APPROVE_OPTION) {
					if (result == SchematicChooser.ERROR_OPTION)
						Log.e("Failed to open schematic.");
					
					return;
				}
				
				controller.openSchematic(chooser.getSelectedFile());
			}
		});
		
		saveItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.saveAll();
			}
		});
		
		exportItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.export();
			}
		});
		
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.exit();
			}
		});
	}
}