package sim.logic;

import logging.Log;
import sim.constants.Constants;
import sim.loading.Linker;
import utils.CircularByteBuffer;
import utils.Tag;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class RNBTTags {

    private Constructor<?> c_NBTTagCompound, c_NBTSizeTracker;
	private Method m_load, m_write;
	
	private final static long MAXSIZE = 2097152L;
	
	// DEBUG
	private Method m_getTagList, m_getCompoundTagAt;

	public RNBTTags(Linker linker) throws NoSuchMethodException, SecurityException {
		
		prepareNBTTagCompound(linker);
		
		Log.i("Preparing NBT-Tags");
	}
	
	private void prepareNBTTagCompound(Linker linker) throws NoSuchMethodException, SecurityException {

        Class<?> NBTTagCompound = linker.getClass("NBTTagCompound");
        Class<?> NBTTagList = linker.getClass("NBTTagList");
        Class<?> NBTSizeTracker = linker.getClass("NBTSizeTracker");
		
		c_NBTTagCompound = NBTTagCompound.getDeclaredConstructor();
		c_NBTSizeTracker = NBTSizeTracker.getDeclaredConstructor(long.class);
		
		m_load = NBTTagCompound.getDeclaredMethod(Constants.NBTTAGCOMPOUND_LOAD, DataInput.class, int.class, NBTSizeTracker);
		m_load.setAccessible(true);
		
		m_write = linker.method("write", NBTTagCompound, DataOutput.class);
		
		// DEBUG
		m_getTagList = linker.method("getTagList", NBTTagCompound, String.class, int.class);
		m_getCompoundTagAt = linker.method("getCompoundTagAt", NBTTagList, int.class);
	}
	
	public Object newInstance() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return c_NBTTagCompound.newInstance();
	}
	
	Object getInstance(DataInput input, int complexity) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Object nbtSizeTracker = c_NBTSizeTracker.newInstance(MAXSIZE);
		Object instance = c_NBTTagCompound.newInstance();
		
		m_load.invoke(instance, input, complexity, nbtSizeTracker);
		
		return instance;
	}
	
	public Object getMinecraftTagFromTag(Tag tag) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		
		CircularByteBuffer cbb = new CircularByteBuffer(CircularByteBuffer.INFINITE_SIZE);
		tag.writePayload(new DataOutputStream(cbb.getOutputStream()));
		Object mcTag = getInstance(new DataInputStream(cbb.getInputStream()), 0);
		
		if (Constants.DEBUG_TAG_COMPOUND)
			System.out.println("In:  " + mcTag);
		
		return mcTag;
	}
	
	public Tag getTagFromMinecraftTag(Object mcTag) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		
		if (Constants.DEBUG_TAG_COMPOUND)
			System.out.println("Out: " + mcTag);
		
		CircularByteBuffer cbb = new CircularByteBuffer(CircularByteBuffer.INFINITE_SIZE);
		m_write.invoke(mcTag, new DataOutputStream(cbb.getOutputStream()));

        return Tag.createCompountTag(new DataInputStream(cbb.getInputStream()));
	}
	
	public Object getTagList(Object tag, String name) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return m_getTagList.invoke(tag, name, 10);
	}
	
	public Object getCompoundTagAtObject(Object tag, int pos) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return m_getCompoundTagAt.invoke(tag, pos);
	}

}
