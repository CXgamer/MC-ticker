package presentation.controllers;

import logging.Log;
import org.xml.sax.SAXException;
import presentation.gui.tiles.Graphic;
import presentation.gui.tiles.Mirror;
import presentation.gui.tiles.TileSet;
import presentation.gui.tiles.TilesXml;
import presentation.main.Constants;
import presentation.objects.Orientation;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileController {

	private List<TileSet> tileSets;
	private Map<Integer, BufferedImage> quickMap;
	
	public TileController(File xmlPath) {
		
		try {
			TilesXml parser = new TilesXml();
			tileSets = parser.parseTiles(xmlPath);
			quickMap = new HashMap<>();
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			
			Log.e("Could not parse XML: " + e.getMessage());
			e.printStackTrace();
        }
	}
	
	private Graphic getGraphic(byte byteId, byte data, Orientation orientation, byte custom) {
		
		short id = (short) (byteId & 0xFF);
		
		Graphic g = new Graphic();
		
		for (TileSet tileSet : tileSets)
			if (tileSet.containsId(id)) {
				g = tileSet.getGraphic(id, data, orientation, custom);
				break;
			}
		
		if (sim.constants.Constants.DEBUG_TILE_IMAGES && g.getName().isEmpty()) {
			Log.w("Tile image not found for id=" + id + ", data=" + data + ", orientation= " + orientation);
			return null;
		}
		
		return g;
	}
	
	private File getFile(String name) {

        return new File(Constants.TILEIMAGES + name + Constants.IMAGEEXTENSION);
	}
	
	private BufferedImage getImage(File imageFile) throws IOException {

        return ImageIO.read(imageFile);
			
	}
	
	private BufferedImage rotate(BufferedImage img, int angle)
	{
		int w = img.getWidth();
		int h = img.getHeight();
		
		BufferedImage rot = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
		
	    Graphics2D g = rot.createGraphics();
	    
	    AffineTransform xform = new AffineTransform(); 
	    xform.translate(0.5*h, 0.5*w);
	    xform.rotate(angle * Math.PI / 180);
	    xform.translate(-0.5*w, -0.5*h);
	    
	    g.transform(xform);
	    g.drawImage(img, 0, 0, w, h, null, null);
	    
///	    g.setColor(Color.BLUE);
///	    char[] chars = new String(String.valueOf(angle)).toCharArray();
///	    g.drawChars(chars, 0, chars.length, 0, 10);
	    
	    return rot;
	}
	
	private BufferedImage mirror(BufferedImage img, Mirror mirror) {
		
		int w = img.getWidth();
		int h = img.getHeight();

		BufferedImage mir = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
		AffineTransform tx;
		
	    switch (mirror) {
		
	    	case HORIZONTAL:
	    		tx = AffineTransform.getScaleInstance(-1, 1);
	    		tx.translate(-w, 0);
	    		break;
	    		
	    	case VERTICAL:
	    		tx = AffineTransform.getScaleInstance(1, -1);
	    		tx.translate(0, -h);
	    		break;	
	    		
	    	default:
	    	case NONE:
	    		tx = new AffineTransform();
	    		break;
	    }
	    AffineTransformOp op = new AffineTransformOp(tx,
	        AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	    mir = op.filter(img, mir);
		    
	    return mir;
	}
	
	private int getHash(byte id, byte data, Orientation orientation) {
		
		byte or;
		
		switch (orientation) {
			case TOP:
				or = 1;
				break;
				
			case FRONT:
				or = 2;
				break;
				
			case RIGHT:
				or = 3;
				break;
				
			default:
				or = 0;
		}
		
		return id << 10 | data << 2 | or;
	}
	
	public BufferedImage getTile(byte id, byte data, Orientation orientation) {
		return getTile(id, data, orientation, (byte) 0);
	}

    public BufferedImage getTile(String name) {

        File file = getFile(name);

        BufferedImage image;

        try {
            image = getImage(file);

        } catch (IOException e) {
            if (sim.constants.Constants.DEBUG_TILE_IMAGES)
                Log.w("Could not read tile image: " + name);
            return null;
        }

        if (image == null) return null;

        return image;
    }
	
	BufferedImage getTile(byte id, byte data, Orientation orientation, byte custom) {
		
		int hash = getHash(id, data, orientation);
		
		if (quickMap.containsKey(hash))
			return quickMap.get(hash);
		
		Graphic g = getGraphic(id, data, orientation, custom);
		
		BufferedImage image = getTile(g.getName());
		
		if (g.getRotation() != 0) {
			image = rotate(image, g.getRotation());
		}
		
		if (g.getMirror() != Mirror.NONE) {
			image = mirror(image, g.getMirror());
		}
		
		quickMap.put(hash, image);
		
		return image;	
	}
}
