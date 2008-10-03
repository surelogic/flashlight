package com.surelogic._flashlight;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Takes events from the out queue and persists them according to an output
 * strategy.
 */
final class Depository extends Thread {

	private final BlockingQueue<List<Event>> f_outQueue;

	private volatile EventVisitor f_outputStrategy;

	Depository(final BlockingQueue<List<Event>> outQueue,
			final EventVisitor outputStrategy) {
		super("flashlight-depository");
		assert outQueue != null;
		f_outQueue = outQueue;
		assert outputStrategy != null;
		f_outputStrategy = outputStrategy;
	}

	private boolean f_finished = false;

	private long f_outputCount = 0;	
	
	private class ClassVisitor extends IdPhantomReferenceVisitor {
		int count = 0;
		
		@Override
		void visit(final ClassPhantomReference r) {
			//System.err.println("Depository: "+r);							
			count = outputFieldDefs(r.getName(), r.getId(), f_outputStrategy);
		}		
	}
	
	private final ClassVisitor classVisitor = new ClassVisitor();
	
	private final Map<String,List<FieldInfo>> fieldDefs = loadFieldInfo();
			
	static class FieldInfo {
		final int id;
		final String declaringType;
		final String name;
		final boolean isStatic, isFinal, isVolatile;
		
		FieldInfo(String line) {
			StringTokenizer st = new StringTokenizer(line);
			id = Integer.parseInt(st.nextToken());
			declaringType = st.nextToken();
			name = st.nextToken();
			isStatic = Boolean.parseBoolean(st.nextToken());
			isFinal  = Boolean.parseBoolean(st.nextToken());
			isVolatile = Boolean.parseBoolean(st.nextToken());				
		}

		public void accept(long declaringType, EventVisitor strategy) {
			strategy.visit(new FieldDefinition(id, declaringType, name, 
					                           isStatic, isFinal, isVolatile));
		}
	}
		
	@Override
	public void run() {
		Store.flashlightThread();
		
		while (!f_finished) {
			try {
				List<Event> buf = f_outQueue.take();
				for(Event e : buf) {
					if (e == FinalEvent.FINAL_EVENT)
						f_finished = true;
					
					e.accept(f_outputStrategy);
					
					if (e instanceof ObjectDefinition) {
						ObjectDefinition od    = (ObjectDefinition) e;
						IdPhantomReference ref = od.getObject();
						ref.accept(od, classVisitor);	
						f_outputCount += (1+classVisitor.count);
					} else {
						f_outputCount++;
					}
				}
				buf.clear();
			} catch (InterruptedException e) {
				Store.logAProblem("depository was interrupted...a bug");
			}
		}
		f_outputStrategy.flush();
		Store.log("depository flushed (" + f_outputCount + " events(s) output)");
	}

	public static Map<String,List<FieldInfo>> loadFieldInfo() {
		String name = StoreConfiguration.getFieldsFile();
		if (name == null) {
			return Collections.emptyMap();
		}
		File f = new File(name);
		if (!f.exists() || !f.isFile()) {
			return Collections.emptyMap();
		}
		Map<String,List<FieldInfo>> map = new HashMap<String,List<FieldInfo>>();
		try {
			Reader r = new FileReader(f);			
			BufferedReader br = new BufferedReader(r);
			String line;
			while ((line = br.readLine()) != null) {
				FieldInfo fi      = new FieldInfo(line);
				List<FieldInfo> l = map.get(fi.declaringType);
				if (l == null) {
					l = new ArrayList<FieldInfo>();
					map.put(fi.declaringType, l);
				}
				l.add(fi);
			}
		} catch (IOException e) {
			Store.logAProblem("Couldn't read field definition file", e);
			map.clear();
		}
		return map;
	}
	
	public int outputFieldDefs(String name, long id, EventVisitor strategy) {
		List<FieldInfo> l = fieldDefs.remove(name);
		if (l == null || l.isEmpty()) {
			return 0;
		}
		int count = 0;
		for(FieldInfo fi : l) {			
			fi.accept(id, strategy);
		}
		return count;
	}

	/**
	 * Sets the output strategy used by the Depository. This method is intended
	 * for <i>test code use only</i> because the {@link Store} configures the
	 * output strategy for this thread upon construction. Tests, however, may
	 * want to avoid output and/or simply count events.
	 * 
	 * @param outputStrategy
	 *            an output strategy.
	 */
	void setOutputStrategy(final EventVisitor outputStrategy) {
		if (outputStrategy == null)
			throw new IllegalArgumentException(
					"outputStrategy must be non-null");
		if (f_outputStrategy != null) {
			/*
			 * Allow the default XML strategy to end properly and close its
			 * file.
			 */
			f_outputStrategy.visit(FinalEvent.FINAL_EVENT);
		}
		f_outputStrategy = outputStrategy;
	}
}
