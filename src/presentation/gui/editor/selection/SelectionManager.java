package presentation.gui.editor.selection;

import presentation.controllers.WorldController;
import presentation.main.Cord3S;
import presentation.objects.ViewData;

/**
 * A manager which manages the 3D selection
 */
public class SelectionManager {

    /**
     * The selection
     */
    private boolean[][][] selection;

    /**
     * Handles positive and negative selections
     */
    private boolean positiveSelection;

    /**
     * The corners of the big square
     */
    private Cord3S start, end;

    /**
     * The first clicked cord, saved for shift clicking and dragging
     */
    private Cord3S prevStartCord;

    /**
     * If anything is selected, to make it possible to skip drawing
     */
    private boolean anythingSelected;

    private final WorldController worldController;

    public SelectionManager(WorldController worldController) {

        this.worldController = worldController;

        clearSelection();
    }

    /**
     * Clears the entire selection
     */
    private void clearSelection() {
        ViewData vd = worldController.getWorldData();
        selection = new boolean[vd.getXSize()][vd.getYSize()][vd.getZSize()];

        anythingSelected = false;
    }

    /**
     * Selects a single block
     * @param cord The cord to be selected
     */
    private void selectPoint(Cord3S cord) {

        anythingSelected = true;

        this.start = cord;
        this.end = cord;

        selectBoundedRectangle(cord, cord);
    }

    private void selectRegion(Cord3S start, Cord3S end) {

        anythingSelected = true;

        selectBoundedRectangle(start, end);

        // This part requires the cords to be sorted
        for (int x = start.x; x <= end.x; x++)
            for (int y = start.y; y <= end.y; y++)
                for (int z = start.z; z <= end.z; z++)
                    selection[x][y][z] = positiveSelection;
    }

    /**
     * Sets the region to the cuboid between these points, sorts them so that all start cords are the smaller ones
     * @param start One of the 2 points
     * @param end The other of the two
     */
    private void selectBoundedRectangle(Cord3S start, Cord3S end) {

        // If we selected a point, skip everything
        if (start == end) {
            this.start = start;
            this.end = end;
            return;
        }

        // Temp values
        short sx, sy, sz, ex, ey, ez;

        // This counter will check if we can avoid creating a new instance
        byte count = 0;

        if (start.x <= end.x) {

            sx = start.x;
            ex = end.x;
            count++;

        } else {

            sx = end.x;
            ex = start.x;
        }

        if (start.y <= end.y) {

            sy = start.y;
            ey = end.y;
            count++;

        } else {

            sy = end.y;
            ey = start.y;
        }

        if (start.z <= end.z) {

            sz = start.z;
            ez = end.z;
            count++;

        } else {

            sz = end.z;
            ez = start.z;
        }

        // When all start cords are smaller or equal
        if (count == 3) {

            this.start = start;
            this.end = end;

            return;
        }

        // If the selection is completely reversed
        if (count == 0) {

            this.start = end;
            this.end = start;
            return;
        }

        // Otherwise just create 2 new instances :(
        this.start = new Cord3S(sx, sy, sz);
        this.end = new Cord3S(ex, ey, ez);
    }

    /**
     * Returns whether the block is selected or not
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return True if the block is selected
     */
    public boolean isSelected(int x, int y, int z) {
        return selection[x][y][z];
    }

    public void startSelection(Cord3S cord, boolean shift, boolean ctrl) {
        if (!ctrl)
            clearSelection();

        positiveSelection = !selection[cord.x][cord.y][cord.z];

        if (shift && prevStartCord != null)

            selectRegion(prevStartCord, cord);

        else {

            prevStartCord = cord;
            selectPoint(cord);
        }

        worldController.repaintAllEditors();
    }

    public void dragSelection(Cord3S cord) {

        selectBoundedRectangle(prevStartCord, cord);
        worldController.repaintAllEditors();
    }

    public void endSelection() {

        selectRegion(start, end);

        worldController.repaintAllEditors();
    }

    public Cord3S getStart() {
        return start;
    }

    public Cord3S getEnd() {
        return end;
    }

    public boolean isAnythingSelected() {
        return anythingSelected;
    }
}
