package com.vodori.blf.pftools;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 * @author	Ben Fagin (Vodori)
 * @version	5/25/11
 */
public class PFRemapperTest {

	@Test
	public void createMapTest() {
		String one = "{one: 1, two:2, three : 3 , one :11}";
		Map<String, Set<String>> map = PFRemapper.createMap(one);

		for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
			String key = entry.getKey();
			
			for (String value : entry.getValue()) {
				System.out.println(key+ " : " +value);
			}
		}
	}
}
