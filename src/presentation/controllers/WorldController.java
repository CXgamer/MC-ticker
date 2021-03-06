package presentation.controllers;

import logging.Log;
import presentation.gui.choosers.SchematicChooser;
import presentation.gui.editor.Editor;
import presentation.gui.editor.entity.EntityManager;
import presentation.gui.editor.layer.LayerManager;
import presentation.gui.editor.selection.SelectionManager;
import presentation.gui.menu.WorldMenu;
import presentation.gui.windows.main.options.IPreferenceChangedListener;
import presentation.gui.windows.world.DrawingWindow;
import presentation.gui.windows.world.NBTviewer;
import presentation.main.Constants;
import presentation.main.Cord2S;
import presentation.main.Cord3S;
import presentation.objects.Block;
import presentation.objects.Orientation;
import presentation.objects.ViewData;
import presentation.tools.Tool;
import sim.constants.Prefs;
import sim.logic.SimWorld;
import utils.Tag;

import java.awt.event.MouseMotionListener;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 * Controlls everything about one world
 */
public class WorldController implements IPreferenceChangedListener {
	
	private ViewData viewData;
	
	private CopyOnWriteArrayList<Editor> editors;
	private WorldMenu worldMenu;
	private TimeController timeController;
	
	private final MainController mainController;
	private final SimController simController;
	
	private NBTviewer nbtViewer;
	private NBTController nbtController;

	private File lastSavedFile;

    private LayerManager layerManager;
    private EntityManager entityManager;
    private SelectionManager selectionManager;

    private long lastUpdateTime;
    private boolean doUpdate;
    private TimerUpdater timerUpdater;

    private int minFrameDelay;

    public WorldController(MainController mainController, SimWorld simWorld, String name, short xSize, short ySize, short zSize) {

		this.mainController = mainController;
		this.simController = new SimController(simWorld);

        initiliseMinFrameDelay();

		viewData = new ViewData(name, xSize, ySize, zSize);
		simController.createNewWorld(xSize, ySize, zSize);

		loadSim();
	}
	
	public WorldController(MainController mainController, SimWorld simWorld, File schematicFile) throws IOException, NoSuchAlgorithmException {

		this.mainController = mainController;
		this.simController = new SimController(simWorld);
		this.lastSavedFile = schematicFile;

        initiliseMinFrameDelay();
		
		Tag schematic = Tag.readFrom(new FileInputStream(schematicFile));
		
		viewData = new ViewData(schematicFile.getName(),
								(short) schematic.findTagByName("Width").getValue(),
								(short) schematic.findTagByName("Height").getValue(),
								(short) schematic.findTagByName("Length").getValue());

		// Loads the world
		simController.setSchematic(schematic);

		loadSim();
	}

    private void initiliseMinFrameDelay() {
        minFrameDelay = 1000 / Preferences.userRoot().getInt(Prefs.EDITOR_MAXFPS, Constants.MAX_FPS);
        mainController.getOptionsController().registerPreferenceListener(Prefs.EDITOR_MAXFPS, this);
    }

	private void loadSim() {

        timerUpdater = new TimerUpdater();
        Thread timerUpdaterThread = new Thread(timerUpdater);
        timerUpdaterThread.start();

		worldMenu = new WorldMenu(this);
		timeController = new TimeController(this);

		nbtViewer = new NBTviewer(mainController.getFrame().getDesktop(), this);
		nbtController = new NBTController(this, nbtViewer);

        layerManager = new LayerManager(mainController);
        entityManager = new EntityManager(this);
        selectionManager = new SelectionManager(this);

        // Adds the world to the menu
        mainController.getFrame().getWindowMenu().addWorldMenu(worldMenu);

        // This will fill ViewData
        timeController.init();

        editors = new CopyOnWriteArrayList<>();
        addNewPerspective(Orientation.TOP);

        setDoUpdate(true);
	}
	
	public void addNewPerspective(Orientation orientation) {
		
		DrawingWindow drawingWindow = new DrawingWindow(mainController.getFrame().getDesktop(), this, orientation);
        Editor editor = drawingWindow.getEditor();

        layerManager.addLayer(editor);
        entityManager.addEditor(editor);

        Tool tool = getMainController().getTool();
        editor.addMouseListener(tool);
        if (tool.hasMouseMotionListener())
            editor.addMouseMotionListener((MouseMotionListener) tool);

        editors.add(editor);
	}
	
	public void drawingWindowClosed(DrawingWindow source) {

		layerManager.removeLayer(source.getEditor());
		entityManager.removeEditor(source.getEditor());

		editors.remove(source.getEditor());
		
		if (editors.isEmpty())
			close();
	}
	
	public void close() {

        for (Editor editor : editors)
            editor.getDaddy().dispose();

		timeController.stopThread();
		
		nbtViewer.dispose();
		
		mainController.getFrame().getWindowMenu().removeWorldMenu(worldMenu);
		mainController.onWorldRemoved(this);
	}
	
	public void setBlock(final int x, final int y, final int z, final char block, boolean update) {

        try {
            setDoUpdate(false);

            timeController.loadCurrentTimeIntoSchematic(true);

            simController.setBlock(x, y, z, Block.getId(block), Block.getData(block), update);

            timeController.updateCurrentSchematic();

            setDoUpdate(true);

        } catch (NullPointerException e) {
            Log.printEntireStackTraceAndBeDoneWithIt(e);
        }
	}
	
	public void onSchematicUpdated() {
        if (!shouldUpdate())
            return;

        doUpdate = false;

		nbtController.onSchematicUpdated();

        entityManager.updateEntities();

        for (Editor editor : editors)
            editor.onSchematicUpdated();

        lastUpdateTime = System.currentTimeMillis();

        doUpdate = true;
	}
	
	public ViewData getWorldData() {
        return viewData;
	}
	
	public MainController getMainController() {
		return mainController;
	}
	
	public TimeController getTimeController() {
		return timeController;
	}
	
	public WorldMenu getWorldMenu() {
		return worldMenu;
	}
	
	public List<Editor> getEditors() {
		return editors;
	}

    public void repaintAllEditors() {
        for (Editor editor : editors)
            editor.repaint();
    }

	private int updateSavedFile() {

		SchematicChooser chooser = new SchematicChooser(new File("schems"));
		chooser.setSelectedFile(new File(getWorldData().getName() + ".schematic"));

		int result = chooser.showOpenDialog(mainController.getFrame());

		if (result == SchematicChooser.APPROVE_OPTION)
			lastSavedFile = chooser.getSelectedFile();

		return result;
	}

	public void save() {

		if (lastSavedFile == null)
			if (updateSavedFile() != SchematicChooser.APPROVE_OPTION)
				return;

		try {
			simController.saveWorld(new FileOutputStream(lastSavedFile));

		} catch (FileNotFoundException e) {

			Log.e("Failed to save world: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void saveAs() {

		if (updateSavedFile() != SchematicChooser.APPROVE_OPTION)
			return;

		try {
			simController.saveWorld(new FileOutputStream(lastSavedFile));

		} catch (FileNotFoundException e) {

			Log.e("Failed to save world: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void revert() {

		if (lastSavedFile == null) {

			Log.e("No file to revert from");
			return;
		}

		try {
			simController.setSchematic(Tag.readFrom(new FileInputStream(lastSavedFile)));

			timeController.init();
			onSchematicUpdated();

		} catch (IOException | NoSuchAlgorithmException e) {

			Log.e("Failed to revert to file: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void onSelectionUpdated(Cord2S cord2D, Cord3S cord3D, Editor source) {

		mainController.onSelectionUpdated(this, cord2D, cord3D, true);
		
		if (source != null)
			for (Editor editor : editors) {
				
				if (!editor.equals(source)) {
					
					editor.selectCord(cord3D);
				}
			}
	}

	public void debug(int x, int y, int z) {

		simController.debug(x, y, z);
	}

	@Override
	public String toString() {
		return viewData.getName();
	}
	
	public SimController getSimController() {
		return simController;
	}

    public LayerManager getLayerManager() {
        return layerManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public void setDoUpdate(boolean doUpdate)  {

        this.doUpdate = doUpdate;

        if (doUpdate)
            onSchematicUpdated();
    }

    synchronized boolean shouldUpdate() {
        if (!doUpdate)
            return false;

        long newTime = System.currentTimeMillis();
        boolean result = newTime - lastUpdateTime >= minFrameDelay;

        if (!result)
            timerUpdater.setTimeTarget(newTime + minFrameDelay);

        return result;
    }

    @Override
    public void preferenceChanged(String preference) {
        minFrameDelay = 1000 / Preferences.userRoot().getInt(Prefs.EDITOR_MAXFPS, Constants.MAX_FPS);
    }

    class TimerUpdater implements Runnable {

        private long timeTarget;

        @Override
        public void run() {

            for (;;) {
                try {
                    while (System.currentTimeMillis() < timeTarget)
                            Thread.sleep(minFrameDelay);

                    onSchematicUpdated();

                    synchronized (this) {
                        wait();
                    }

                } catch (InterruptedException e) {
                    Log.w("Frame updater thread was awoken from its sleep.");
                }
            }
        }

        public synchronized void setTimeTarget(long timeTarget) {
            this.timeTarget = timeTarget;
            notify();
        }
    }
}
