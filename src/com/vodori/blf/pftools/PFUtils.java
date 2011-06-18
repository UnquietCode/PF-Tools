package com.vodori.blf.pftools;

import java.io.*;
import java.util.*;

/**
 * @author Benjamin Fagin
 * @version 06-05-2011
 */
public final class PFUtils {
	public static final String NL = System.getProperty("line.separator");
	public static final String BOM = "\uFEFF";
	private static int RANDOM_SEPARATOR;

	static {
		Random gen = new Random();
		RANDOM_SEPARATOR = gen.nextInt(Integer.MAX_VALUE-1223334) + 11223334;
	}


	public static Scanner getReader(String file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		return new Scanner(isr);
	}

	public static String getCode(String line) {
		return getParts(line)[0];
	}

	public static String getMessage(String line) {
		return getParts(line)[1];
	}

	public static String[] getParts(String line) {
		//TODO leading whitespace is lost. How to restore?
		line = line.replaceAll("\\\\", RANDOM_SEPARATOR+"\\\\"+NL);

		Properties properties = new Properties();
		try {
			properties.load(new StringReader(line));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		for (Object prop : properties.keySet()) {
			String key = (String) prop;
			String value = (String) properties.get(prop);

			key = key.replaceAll(""+RANDOM_SEPARATOR, "\\\\");
			value = value.replaceAll(""+RANDOM_SEPARATOR, "\\\\");

			return new String[]{key, value};
		}

		return null;
	}

	public static boolean isMultiLine(String line) {
		// no null check please

		line = line.trim();

		// only odd numbers of slashes denote multiline, so remove every pair
		while (line.endsWith("\\\\")) {
			line = line.substring(0, line.length()-2);
		}
		
		return line.endsWith("\\");
	}

	public static boolean isComment(String line) {
		// no null check please

		line = line.trim();
		return (line.startsWith("#") || line.startsWith("!"));
	}

	/*
		Just like it sounds, adds a value to a multimap build from a key
		and a set of values. If this is the first value for the key a
		new java.util.HashSet is created.
	 */
	public static <K,V> void addToSet(K key, V value, Map<K, Set<V>> map) {
		Set<V> set = map.containsKey(key)
				   ? map.get(key)
				   : new HashSet<V>();

		set.add(value);
		map.put(key, set);
	}
}