package com.surelogic._flashlight;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;

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
			count = outputClassDefs(r.getName(), r.getId(), f_outputStrategy);
		}		
	}
	
	private final ClassVisitor classVisitor = new ClassVisitor();

	private final Map<String,List<ClassInfo>> classDefs = loadClassInfo();	
	
	static class ClassInfo extends AbstractList<ClassInfo> {
		final String fileName;
		final String className;
		final SiteInfo[] sites;
		final FieldInfo[] fields;
		
		ClassInfo(String file, String clazz, SiteInfo[] sites, FieldInfo[] fields) {
			fileName = file;
			className = clazz;
			this.sites = sites;
			this.fields = fields;
		}

		@Override
		public ClassInfo get(int index) {
			if (index == 0) {
				return this;
			}
			throw new NoSuchElementException();
		}

		@Override
		public int size() {
			return 1;
		}
	}
	
	static class SiteInfo {
		final long id;
		final String memberName;
		final int line;
		
		SiteInfo(long id, String name, int line) {
			this.id = id;
			memberName = name;
			this.line = line;
		}
		
		void accept(long declaringType, EventVisitor strategy, ClassInfo info) {
			strategy.visit(new StaticCallLocation(id, memberName, line, 
					                              info.fileName, declaringType));
		}
	}
	
	static class StringTable extends HashMap<String,String> {
		public String intern(String s) {
			String cached = this.get(s);
			if (cached != null) {
				return cached;
			}
			this.put(s, s);
			return s;
		}
		
	}
	
	static class FieldInfo {
		final int id;
		final String declaringType;
		final String name;
		final boolean isStatic, isFinal, isVolatile;
		
		FieldInfo(final StringTable strings, String line) {
			StringTokenizer st = new StringTokenizer(line);
			id = Integer.parseInt(st.nextToken());
			declaringType = strings.intern(st.nextToken());
			name = strings.intern(st.nextToken());
			isStatic = Boolean.parseBoolean(st.nextToken());
			isFinal  = Boolean.parseBoolean(st.nextToken());
			isVolatile = Boolean.parseBoolean(st.nextToken());				
		}

		void accept(long declaringType, EventVisitor strategy) {
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
					if (e == null) {
						continue;
					}
					if (e == FinalEvent.FINAL_EVENT) {
						f_finished = true;
						//System.err.println("Outputting final time");
						new Time().accept(f_outputStrategy);
					}
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
		
		if (StoreConfiguration.debugOn()) {
			f_outputStrategy.printStats();
		}
	}

	private static Map<String,List<FieldInfo>> loadFieldInfo(final StringTable strings) {
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
				FieldInfo fi      = new FieldInfo(strings, line);
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
	
	int outputFieldDefs(long id, EventVisitor strategy, ClassInfo info) {
		FieldInfo[]l = info.fields;
		if (l == null || l.length == 0) {
			return 0;
		}
		int count = 0;
		for(FieldInfo fi : l) {			
			fi.accept(id, strategy);
			count++;
		}
		return count;
	}

	int outputClassDefs(String name, long id, EventVisitor strategy) {
		List<ClassInfo> infos = classDefs.remove(name);
		if (infos == null) {
			/*
			if (name.startsWith("java")) {
				return 0;
			}
			if ("com.surelogic._flashlight.ObservedField$FieldInfo".equals(name)) {
				return 0;
			}
			throw new IllegalArgumentException();
			*/
			return 0;
		}
		int events = 0;
		for(ClassInfo info : infos) {
			events += outputFieldDefs(id, strategy, info);
			for(SiteInfo site : info.sites) {
				//System.err.println("Site "+site.id+" at line "+site.line);
				site.accept(id, strategy, info);
				events++;			
			}
		}
		return events;
	}
		
	private static Map<String,List<ClassInfo>> loadClassInfo() {
		String name = StoreConfiguration.getSitesFile();
		File f;
		if (name != null) {
			f = new File(name);
		} else {
			// Try to use fields file to find the sites file
			name = StoreConfiguration.getFieldsFile();
			if (name == null) {
				return Collections.emptyMap();
			}
			f = new File(name);
			f = new File(f.getParentFile(), "sitesfile.txt");
		}
		final SitesReader sitesReader = loadFileContents(f, new SitesReader());
		final Map<String, List<ClassInfo>> classesMap = sitesReader.getMap();
		final Map<String, List<FieldInfo>> fieldsMap = sitesReader.getFieldsMap();
		
		/* Create ClassInfo objects for the remaining site-less classes.  These
		 * are classes that are not instrumented, but possibly referenced by
		 * instrumented code.  (We know they might be used because the fields file
		 * only contains fields that are actually used somewhere in instrumented
		 * code.)
		 */
		for (final Map.Entry<String, List<FieldInfo>> entry : fieldsMap.entrySet()) {
		  final String classname = entry.getKey();
		  final List<FieldInfo> finfo = entry.getValue();

      /* Copied from makeClassInfo() below */
		  final FieldInfo[] fields = finfo == null ? noFields : finfo.toArray(noFields);
		  final ClassInfo info = new ClassInfo("<unknown>", classname, noSites, fields);
      List<ClassInfo> infos = classesMap.get(classname);
      if (infos == null) {
        classesMap.put(classname, info);
      } else {
        infos.add(info);
      }
		}

    return classesMap.isEmpty() ? Collections.<String,List<ClassInfo>>emptyMap() : classesMap;
	}
	
	private interface LineHandler {
		void readLine(String line);		
	}
	
	private static final SiteInfo[] noSites = new SiteInfo[0];
	private static final FieldInfo[] noFields = new FieldInfo[0];	
	
	static class SitesReader implements LineHandler {
		final StringTable strings = new StringTable(); 
		final Map<String,List<FieldInfo>> fields = loadFieldInfo(strings);		
		final Map<String,List<ClassInfo>> classes = new HashMap<String,List<ClassInfo>>();
		List<SiteInfo> sites = new ArrayList<SiteInfo>();
		String lastFileName;
		String lastClassName;
		String lastMemberName;
		
		public void readLine(String line) {
			final StringTokenizer st = new StringTokenizer(line);
			long id = Long.parseLong(st.nextToken());
			String file = st.nextToken(); // FIX intern
			String qname = st.nextToken(); // FIX intern
			String member = st.nextToken(); 
			int lineNo = Integer.parseInt(st.nextToken());
			if (member.equals(lastMemberName)) {
				member = lastMemberName;
			} else {
				member = strings.intern(member);
				lastMemberName = member;
			}
			if (!file.equals(lastFileName) || !qname.equals(lastClassName)) {
				makeClassInfo();
				file = strings.intern(file);
				qname = strings.intern(qname);
				lastFileName = file;
				lastClassName = qname;
			}
			SiteInfo site = new SiteInfo(id, member, lineNo);
			sites.add(site);
		}

		private void makeClassInfo() {
			if (lastClassName != null) {
				List<FieldInfo> finfo = this.fields.remove(lastClassName);
				FieldInfo[] fields = finfo == null ? noFields : finfo.toArray(noFields);
				ClassInfo info = new ClassInfo(lastFileName, lastClassName, 
						                       sites.toArray(noSites), fields);	
				List<ClassInfo> infos = classes.get(lastClassName);
				if (infos == null) {
					classes.put(lastClassName, info);
				} else {
					if (infos instanceof ClassInfo) {
						ClassInfo firstInfo = (ClassInfo) infos;
						infos = new ArrayList<ClassInfo>();
						classes.put(lastClassName, infos);
						infos.add(firstInfo);
					}
					infos.add(info);
				}
				sites.clear();
			}
		}
		
		public Map<String, List<ClassInfo>> getMap() {
			makeClassInfo();
			return classes;
//			return classes.isEmpty() ? Collections.<String,List<ClassInfo>>emptyMap() : classes;
		}
		
		public Map<String, List<FieldInfo>> getFieldsMap() {
		  return fields.isEmpty() ? Collections.<String, List<FieldInfo>>emptyMap() : fields;
		}
	}
	
	private static <T extends LineHandler> T loadFileContents(File f, T handler) {
		if (!f.exists() || !f.isFile()) {
			if (StoreConfiguration.debugOn()) {
				System.err.println("Can't read: "+f.getName());
			}
			return handler;
		}
		try {
			Reader r;
			if (f.getName().endsWith(".gz")) {
				FileInputStream fin = new FileInputStream(f);
				GZIPInputStream gzip = new GZIPInputStream(fin);
				r = new InputStreamReader(gzip);
			} else {
				r = new FileReader(f);						
			}
			BufferedReader br = new BufferedReader(r);
			String line;
			while ((line = br.readLine()) != null) {
				handler.readLine(line);
			}
		} catch (IOException e) {
			Store.logAProblem("Couldn't read definition file"+f.getName(), e);
		}
		return handler;
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
