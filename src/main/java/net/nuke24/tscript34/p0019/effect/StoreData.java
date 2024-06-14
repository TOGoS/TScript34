package net.nuke24.tscript34.p0019.effect;

/**
 * Store a bit of data.
 * No result.
 */
public class StoreData {
	public static final String SECTOR_SCRIPT_LOCAL = "x:script-lacal";
	
	/**
	 * Identifier for later use in script.
	 * If a hash-based URN, then it should match the hash of the data.
	 */
	public final String id;
	/**
	 * Name of CCouch store sector, if applicable.
	 * Some names are special.
	 * SECTOR_SCRIPT_LOCAL means do not store in system repository,
	 * but only for the duration of this script.
	 */
	public final String sectorName;
	/**
	 * An object representing the data to be stored.
	 * Interpretation depends on type.
	 * At a minimum, byte[] should be supported.
	 * byte[], String, and Concatenation<byte|char> are also good
	 * choices, assuming UTF-8 encoding.
	 * URIResources or Symbols may be supported,
	 * in which case the 
	 */
	public final Object data;
	public StoreData(String id, String sectorName, Object data) {
		this.id = id;
		this.sectorName = sectorName;
		this.data = data;
	}
}
