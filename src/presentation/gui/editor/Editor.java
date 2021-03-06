package presentation.gui.editor;

import presentation.controllers.WorldController;
import presentation.gui.editor.block.BlockPanel;
import presentation.gui.editor.selection.SelectionPanel;
import presentation.main.Constants;
import presentation.main.Cord3S;
import presentation.objects.Orientation;
import presentation.objects.ViewData;
import sim.constants.Prefs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

/**
 * Container for logic regarding the editor. Handles:
 *  - Scaling
 *  - Layers
 *  - Selection
 */
public class Editor extends JLayeredPane {

    /**
     * The size of one tile, 16x16 + 1 pixel border
     */
    public static final byte SIZE = 17;

    private static final int BLOCK_INDEX = 10;
    public static final int LAYER_INDEX = 20;
    public static final int ENTITY_INDEX = 30;
    private static final int SELECTION_INDEX = 40;
    private static final int CURSOR_INDEX = 50;

    private final WorldController worldController;

    /**
     * The orientation of this panel
     */
    private final Orientation orientation;

    /**
     * Which height of layer we're on
     */
    private short layer;

    /**
     * The scale of the editor
     */
    private float scale;

    /**
     * The width and height of the editor panel in tiles, depends on the orientation
     */
    private short worldWidth, worldHeight;

    /**
     * The width and height of the editor panel in pixels
     */
    private int pixelWidth;
    private int pixelHeight;

    /**
     * The panel containing the mouse cursor block
     */
    private CursorPanel cursorPanel;

    /**
     * The panel containing the selected blocks
     */
    private SelectionPanel selectionPanel;

    private BlockPanel blockPanel;

    private final JInternalFrame parent;

    public Editor(WorldController worldController, JInternalFrame parent, Orientation orientation) {
        this(worldController, parent, (short) 0,
                Preferences.userRoot().getFloat(Prefs.EDITOR_DEFAULTZOOM, Constants.DEFAULTZOOM), orientation);
    }

    public Editor(WorldController worldController, JInternalFrame parent, short layer, float scale, Orientation orientation) {
        super();

        this.worldController = worldController;
        this.orientation = orientation;
        this.layer = layer;
        this.parent = parent;

        extractWorldDimensions();
        setScale(scale);

        blockPanel = new BlockPanel(this);
        setLayer(blockPanel, BLOCK_INDEX);
        add(blockPanel);

        if (parent != null) {
            cursorPanel = new CursorPanel(worldController.getMainController(), this);
            setLayer(cursorPanel, CURSOR_INDEX);
            add(cursorPanel);

            selectionPanel = new SelectionPanel(worldController.getMainController(), this);
            setLayer(selectionPanel, SELECTION_INDEX);
            add(selectionPanel);

            addMouseMotionListener(new MouseMoveHandler());
            addMouseListener(new MouseHandler());
        }

        onSchematicUpdated();

        setPreferredSize(new Dimension(pixelWidth, pixelHeight));
    }

    /**
     * Grabs the correct sizes for the width and height dimensions, depending on the orientation
     */
    private void extractWorldDimensions() {

        ViewData viewData = worldController.getWorldData();

        switch (orientation) {

            case TOP:
                worldWidth = viewData.getXSize();
                worldHeight = viewData.getZSize();
                break;

            case FRONT:
                worldWidth = viewData.getXSize();
                worldHeight = viewData.getYSize();
                break;

            case RIGHT:
                worldWidth = viewData.getZSize();
                worldHeight = viewData.getYSize();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics;
        g.scale(scale, scale);
    }

    /**
     * Renders the current image without selection nor layers, used for export
     * @return A BufferedImage containing said image
     */
    public BufferedImage getImage() {

        BufferedImage img = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setClip(0, 0, pixelWidth, pixelHeight);
        paint(g);

        return img;
    }

    /**
     * Sets the scale of the image, a scale of 1.0f will show the images as they are
     * @param scale The scale
     */
    public void setScale(float scale) {
        this.scale = scale;

        pixelWidth = (int) (worldWidth * SIZE * scale);
        pixelHeight = (int) (worldHeight * SIZE * scale);

        setPreferredSize(new Dimension(pixelWidth, pixelHeight));
    }

    /**
     * Gets the scale of the image, a scale of 1.0f will show the images as they are
     * @return The scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * Sets the current height of this layer, should not exceed the max nor be below 0.
     * Must be called after entityManager is initialized
     * @param layer The layer height
     */
    public void setLayerHeight(short layer) {

        this.layer = layer;

        worldController.getLayerManager().updateLayer(this);

        getWorldController().getEntityManager().checkVisibility(this);

        repaint();
    }

    /**
     * Gets the current height of this layer, will not exceed the max nor be below 0
     * @return The layer height
     */
    public short getLayerHeight() {
        return layer;
    }

    /**
     * Selects the 3D dimensional cord in the editor
     * @param cord The cord
     */
    public void selectCord(Cord3S cord) {
        cursorPanel.selectCord(cord);
    }

    /**
     * Returns the tile width of the editor
     * @return The width
     */
    public short getEditorWidth() {
        return worldWidth;
    }

    /**
     * Returns the tile height of the editor
     * @return The height
     */
    public short getEditorHeight() {
        return worldHeight;
    }

    /**
     * Gets the orientation of the editor
     * @return The orientation
     */
    public Orientation getOrientation() {
        return orientation;
    }

    public WorldController getWorldController() {
        return worldController;
    }

    /**
     * Returns the selection panel
     * @return The selection panel
     */
    public SelectionPanel getSelectionPanel() {
        return selectionPanel;
    }

    public void onSchematicUpdated() {
        blockPanel.clearBuffer();
        repaint();
    }

    public JInternalFrame getDaddy() {
        return parent;
    }

    public boolean isPreview() {
        return (parent == null);
    }

    /**
     * Handles updating the selection of the mouse cursor
     */
    private class MouseMoveHandler extends MouseMotionAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            cursorPanel.onSelectionUpdated((short) e.getX(), (short) e.getY());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            cursorPanel.onSelectionUpdated((short) e.getX(), (short) e.getY());
        }
    }

    /**
     * Handles making the selection (in)visible when moving in and out of the editor
     */
    private class MouseHandler extends MouseAdapter {
        @Override
        public void mouseExited(MouseEvent e) {
            cursorPanel.selectCord(null);

            worldController.onSelectionUpdated(null, null, Editor.this);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            cursorPanel.setVisible(true);
        }
    }
}