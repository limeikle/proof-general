package ed.inf.proofgeneral.pgip;

import org.eclipse.core.resources.IMarker;

/**
 * The types of fatalities in PGIP <errrorresponse> messages from the prover.
 */
public enum Fatality {
	/** an info.			generates a marker in Eclipse */
	INFO      ("info"),
	/** a warning.			generates a marker in Eclipse */
	WARNING   ("warning"),
	/** an error message	generates a marker in Eclipse */
	NONFATAL  ("nonfatal"),
	/** PGIP/script command failed.	may generate a marker */
	FATAL     ("fatal"),
	/** prover exit condition */
	PANIC     ("panic"),
	/** a log message.		logged invisibly */
	LOG       ("log"),
	/** a debug message.	sent to console and logged */
	DEBUG     ("debug");

	private final String pgipname;

	private Fatality(String name) {
		pgipname = name.intern();
	}

	public String pgipname() {
		return pgipname;

	}

	/**
	 * @return true if this fatality indicates that the last command failed.
	 */
	public boolean commandFailed() {
		return this == FATAL || this == PANIC;
	}

	/**
	 * @return true if this fatality indicates that the associated error message
	 * should be entered into the system log file.
	 */
	public boolean log() {
		return this == LOG || this == DEBUG;
	}

	public int markerSeverity() {
		switch (this) {
		case INFO:     return IMarker.SEVERITY_INFO;
		case WARNING:  return IMarker.SEVERITY_WARNING;
		case NONFATAL: return IMarker.SEVERITY_ERROR;
		case FATAL:    return IMarker.SEVERITY_ERROR;
		case PANIC:    return IMarker.SEVERITY_ERROR;
		case LOG:      return IMarker.SEVERITY_INFO;
		case DEBUG:    return IMarker.SEVERITY_INFO;
		default:       assert false : "invalid Enum input " + this.toString();
		               return -1; // gives "Other" in Problems View
		}
	}

	/**
	 * Convert the given PGIP fatality name into a fatality value.
	 * @param str
	 * @return the fatality value or NONFATAL if the argument was null or an invalid fatality name.
	 */
	public static Fatality fromString (String str) {
		if (str != null) {
			String testf = str.toLowerCase().intern();
			for (Fatality f : Fatality.values()) {
				if (testf == f.pgipname) {
					return f;
				}
			}
		}
		return NONFATAL;
	}
}