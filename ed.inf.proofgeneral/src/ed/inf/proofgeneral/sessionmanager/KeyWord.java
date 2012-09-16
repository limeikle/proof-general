package ed.inf.proofgeneral.sessionmanager;

/**
 * A typed keyword.
 */
public class KeyWord {
    public final String word;
    public final String type;
    public final NameSpace nameSpace;
    /**
     *
     * @param word
     * @param type
     * @param ns
     */
    public KeyWord(String word, String type, NameSpace ns) {
        this.word = word.intern();
        this.type = type.intern();
        this.nameSpace = ns;
    }

    public boolean equals(Object arg0) {
        if (arg0==this) {
			return true;
		}
        if (!(arg0 instanceof KeyWord)) {
			return false;
		}
        KeyWord w = (KeyWord) arg0;
        return (w.word==word && w.type==type);
    }

    public int hashCode() {
    	 return word.hashCode() + 7 * type.hashCode();
    }

	/**
	 * @return fully qualified name for this keyword.
	 */
	public String getFullName() {
		return nameSpace.getRelativeName(null, word);
	}
	/**
	 * @return relative name for this keyword, treating given namespace as parent
	 * @param ns - context for this keyword; should be a parent of the word, or null for
	 * the root name space.
	 */
	public String getRelativeName(NameSpace ns) {
		return nameSpace.getRelativeName(ns, word);
	}

}