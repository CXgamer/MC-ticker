package sim.logic;

import logging.Log;
import sim.constants.Constants;
import sim.loading.Linker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

class RProfiler {

    private Object profiler;
	private Field f_profilingMap;
	private Method m_startSection, m_stopSection;

	private static RProfiler instance;
	
	public RProfiler(Linker linker) throws NoSuchFieldException, SecurityException, InstantiationException, IllegalAccessException, NoSuchMethodException {
		
		prepareProfiler(linker);

		instance = this;
		
		Log.i("Preparing MC's Profiler");
	}
	
	private void prepareProfiler(Linker linker) throws NoSuchFieldException, SecurityException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        Class<?> profiler1 = linker.getClass("Profiler");
		profiler = profiler1.newInstance();
		f_profilingMap = linker.field("profilingMap", profiler1);
		
		Field f_profilingEnabled = linker.field("profilingEnabled", profiler1);
		f_profilingEnabled.setBoolean(profiler, true);
		
		m_startSection = linker.method("startSection", profiler1, String.class);
		m_stopSection = linker.method("endSection", profiler1);
	}
	
	public void testProfiler(String msg) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		m_startSection.invoke(profiler, msg);
		m_stopSection.invoke(profiler);
	}

	public static void print() {

		try {
			instance.printOutput();

		} catch (IllegalArgumentException | IllegalAccessException e) {

			Log.e("Failed to print MC profiler: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
    void printOutput() throws IllegalArgumentException, IllegalAccessException {
		
		System.out.println("\nMINECRAFT PROFILER:");
		
		HashMap<String, Long> profilingMap = (HashMap<String, Long>) f_profilingMap.get(profiler);
		
		Iterator<Entry<String, Long>> i = profilingMap.entrySet().iterator();
		StringBuilder sb = new StringBuilder();
		
		while (i.hasNext()) {
			
			Entry<String, Long> entry = i.next();
			
			String key = entry.getKey();
			sb.append(key);
			
			int extraSpace = Constants.OUTPUT_INDENT - key.length();
			for (int col = 0; col < extraSpace; col++)
				sb.append(' ');
			
			sb.append(entry.getValue());
			sb.append('\n');
		}
		
		System.out.println(sb.toString());
		
	}
	
	public Object getInstance() {
		return profiler;
	}
}
