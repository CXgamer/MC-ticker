package presentation.blocks;

import logging.Log;
import presentation.main.Cord3S;
import presentation.objects.Orientation;

public class BlockLogic {

    private byte id;
    private String name;

    private byte iconData;
    private Orientation iconOrientation;

    private byte rotationMask, rotationMin, rotationMax, rotationIncrease;

    private Cord3S[] sides;

    private boolean hidden;

//	private byte clickMask, clickMin, clickMax, clickIncrease;

    public BlockLogic(byte id) {
        this(id, false);
    }

    public BlockLogic(byte id, boolean hidden) {
        this.id = id;

        iconOrientation = Orientation.TOP;
        this.hidden = hidden;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIconData(byte iconData) {
        this.iconData = iconData;
    }

    public void setIconOrientation(Orientation iconOrientation) {
        this.iconOrientation = iconOrientation;
    }

    public void setRotationMask(byte rotationMask) {
        this.rotationMask = rotationMask;
        this.rotationMax = rotationMask;
        this.rotationIncrease = getIncreaseFromMask(rotationMask);
    }

    public void setRotationMin(byte rotationMin) {
        this.rotationMin = rotationMin;
    }

    public void setRotationMax(byte rotationMax) {
        this.rotationMax = rotationMax;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setSides(String sidesString) {

        char[] chars = sidesString.toCharArray();
        sides = new Cord3S[chars.length];
        Cord3S cord;

        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case 'd':
                    cord = new Cord3S(0, -1, 0);
                    break;

                case 'u':
                    cord = new Cord3S(0, 1, 0);
                    break;

                case 'n':
                    cord = new Cord3S(0, 0, -1);
                    break;

                case 's':
                    cord = new Cord3S(0, 0, 1);
                    break;

                case 'e':
                    cord = new Cord3S(1, 0, 0);
                    break;

                case 'w':
                    cord = new Cord3S(-1, 0, 0);
                    break;

                default:
                    Log.i("Unrecognized 'sides' character: " + chars[i]);
                    cord = null;

            }

            sides[i] = cord;
        }

    }

//	public void setClickMask(byte clickMask) {
//		this.clickMask = clickMask;
//		this.clickMax = clickMask;
//		this.clickIncrease = getIncreaseFromMask(clickMask);
//	}
//	
//	public void setClickMin(byte clickMin) {
//		this.clickMin = clickMin;
//	}
//
//	public void setClickMax(byte clickMax) {
//		this.clickMax = clickMax;
//	}

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte getIconData() {
        return iconData;
    }

    public Orientation getIconOrientation() {
        return iconOrientation;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isSideDependent() {
        return sides != null;
    }
	
	public Cord3S getSide(byte data) {
		if (!isSideDependent())
			return null;

        int index = (data - rotationMin) / rotationIncrease;

        if (index < 0 || index >= sides.length) {
            Log.e("Internal error: could not rotate block: " + index);
            return null;
        }
		
		return sides[index];
	}
	
	byte getIncreaseFromMask(byte mask) {
		
		for (byte b = 1; b <= 8; b *= 2) {
			
			if ((mask & b) != 0)
				return b;
		}
		
		return 0;
	}
	
	private byte increaseData(byte data, byte mask, byte increase, byte min, byte max) {

		byte increasedData = (byte) ((data & mask) + increase);
		
		if (increasedData > max)
			increasedData = min;
		
		return (byte) (data & ~mask | increasedData & mask);
	}
	
	private byte decreaseData(byte data, byte mask, byte decrease, byte min, byte max) {

		byte decreasedData = (byte) ((data & mask)- decrease);
		
		if (decreasedData < min)
			decreasedData = max;
		
		return (byte) (data & ~mask | decreasedData & mask);
	}
	
	public byte rotate(byte data, boolean forward) {
		
		if (forward)
			return increaseData(data, rotationMask, rotationIncrease, rotationMin, rotationMax);
			
		else
			return decreaseData(data, rotationMask, rotationIncrease, rotationMin, rotationMax);
	}
	
//	public byte click(byte data, boolean forward) {
//		
//		if (forward)
//			return increaseData(data, clickMask, clickIncrease, clickMin, clickMax);
//		else
//			return decreaseData(data, clickMask, clickIncrease, clickMin, clickMax);
//	}
}
