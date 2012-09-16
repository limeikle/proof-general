package ed.inf.proofgeneral.sessionmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ed.inf.utils.datastruct.TreeWalker.Tree;

// da: COMMIT note:
// improve and simplify representation: use sets of strings indexed by type,
// generate keywords on extraction.  Sets alllow faster lookup.

/**
 * This class defines how Proof General handles namespaces. Namespaces hold
 * collections of (typed) keywords (not linked to anything here). They can hold
 * sub-namespaces. A namespace is always of type theory. A NameSpace is
 * shallow-immutable.
 * 
 * @author Daniel Winterstein
 * @author David Aspinall
 */
// IDEA: Dan: could we use XML/XPath namespaces?
// TODO da: support removal and addition here.
// TODO da: fix concurrency; allow concurrent access
// Unify this with Prover Knowledge
public class NameSpace extends HashMap<String, NameSpace> implements Tree {

	public static final String GLOBAL = "*top-level*"; // NB: mustn't be the
	// name of any theory!

	/**
	 * Separate sub-namespaces. eg. global.sub.subsub
	 */
	public final String delimiter = "."; // TODO: can be parameter

	public final String name;
	public final NameSpace parent;
	// private final ProverSyntax syntax;

	/**
	 * The keywords in this namespace (excluding sub-namespaces). This is
	 * indexed on the type.
	 */
	public final HashMap<String, TreeSet<String>> keywords = new HashMap<String, TreeSet<String>>();

	/**
	 * Create a new name space.
	 * 
	 * @param name
	 *            (will be interned)
	 * @param parent -
	 *            must not be null
	 */
	public NameSpace(String name, NameSpace parent) {
		this.name = name.intern();
		this.parent = parent;
		// this.syntax = parent.syntax;
	}

	/**
	 * Create a new global name space.
	 */
	public NameSpace() {
		this.name = GLOBAL;
		// this.syntax = syntax;
		parent = null;
	}

	/**
	 * Return a qualified name relative to the given root.
	 * 
	 * @param relativeroot
	 *            if null, will get fully qualified name
	 * @return name relative to given root
	 */
	public String getRelativeName(NameSpace relativeroot, String id) {
		if (relativeroot == this || parent == null) {
			return id;
		}
		return parent.getRelativeName(relativeroot, name + delimiter + id);
	}

	/**
	 * The sub-namespaces
	 * 
	 * @see ed.inf.utils.datastruct.TreeWalker.Tree#getChildren()
	 */
	public List getChildren() {
		List<NameSpace> kids = new ArrayList<NameSpace>();
		kids.addAll(values());
		return kids;
	}

	public Collection subSpaces() {
		return values();
	}

	/**
	 * Return keywords of the given type, included words inside nested spaces.
	 * The returned array is not backed by the name space, so modifications in
	 * the array will not affect the name space. The order of keywords returned
	 * is unspecified.
	 * 
	 * @param type
	 *            (null = all)
	 * @param depth -
	 *            depth of nested spaces to consider
	 * @param fullNames -
	 *            if true, will return fully qualified names.
	 */
	public String[] getKeywordNames(String type, boolean fullNames, int depth) {
		List<KeyWord> keywds = new ArrayList<KeyWord>(); // TODO: size
		// estimate
		getKeywords(type, depth, keywds);

		String[] skws = new String[keywds.size()];
		int i = 0;
		for (KeyWord kw : keywds) {
			skws[i++] = kw.getRelativeName(fullNames ? null : this);
		}
		return skws;
	}

	public String[] getKeywordNames(String type, boolean fullNames) {
		return getKeywordNames(type, fullNames, Integer.MAX_VALUE);
	}

	public List<KeyWord> getKeywords(String type, int depth, List<KeyWord> list) {
		if (type == null) {
			for (String atype : keywords.keySet()) {
				for (String k : keywords.get(atype)) {
					list.add(new KeyWord(k, atype, this));
				}
			}
		} else {
			String ftype = type.intern();
			if (keywords.containsKey(ftype)) {
				for (String k : keywords.get(ftype)) {
					list.add(new KeyWord(k, ftype, this));
				}
			}
		}
		// Gather from sub spaces
		if (depth > 1) {
			for (NameSpace ns : values()) {
				ns.getKeywords(type, --depth, list);
			}
		}
		return list;
	}

	/**
	 * Delete keyword locally
	 * 
	 * @param type
	 * @param word
	 */
	public void delKeyword(String type, String word) {
		Set<String> keystype = keywords.get(type);
		if (keystype != null) {
			keystype.remove(word);
		}
	}

	/**
	 * Add keyword locally
	 * 
	 * @param type
	 * @param word
	 */
	public void setKeyword(String type, String word) {
		TreeSet<String> keystype = keywords.get(type);
		if (keystype == null) {
			keystype = new TreeSet<String>();
			keywords.put(type, keystype);
		}
		keystype.add(word);
	}

	/**
	 * Search for a given name of any type in this name space.
	 * 
	 * @param word
	 * @return a list of keywords with the given name
	 */
	public KeyWord[] getKeywords(String word) {
		ArrayList<KeyWord> kwds = new ArrayList<KeyWord>();
		for (String atype : keywords.keySet()) {
			if (keywords.get(atype).contains(word)) {
				kwds.add(new KeyWord(word, atype, this));
			}
		}
		return kwds.toArray(new KeyWord[kwds.size()]);
	}

	/**
	 * Return a keyword for the given word, if one can be found of any type
	 * 
	 * @param word
	 * @return a keyword (which contains its type), or null
	 */
	public KeyWord getKeyword(String word) {
		KeyWord[] kwds = getKeywords(word);
		if (kwds.length > 0) {
			return kwds[0];
		}
		return null;
	}

	/**
	 * Return an array of keywords matching the given word
	 */

	/**
	 * Get (and maybe make) a sub-namespace given a subtheory (perhaps
	 * multi-component path) name. Automatically adds keywords for added spaces
	 * under type theory.
	 * 
	 * @param subtheory
	 * @return a name space contained in this one for the path subtheory
	 */
	public NameSpace getSubspace(String subtheory) {
		return getMakeSubspace(subtheory, true);
	}

	public boolean hasSubspace(String subtheory) {
		return getMakeSubspace(subtheory, false) != null;
	}

	private NameSpace getMakeSubspace(String subtheory, boolean make) {
		assert subtheory != null : "Null argument for theory name not allowed";
		if (subtheory.equals("")) {
			return this;
		}
		int delim = subtheory.indexOf(delimiter);
		boolean hasdelim = delim != -1;
		String prefix;
		if (hasdelim) {
			prefix = subtheory.substring(0, delim);
		} else {
			prefix = subtheory;
		}
		// HACK: for consistency with IdView, we allow qualified lookups here
		// which
		// are rooted at the name of this name space, e.g.
		// Transitive_Closure.rtrancl
		// succeeds in Transitive_Closure name space. This has ambiguous
		// behaviour
		// for nested elements with same name, e.g. Datatype.Datatype.foo.
		if (prefix.equals(name)) {
			if (hasdelim) {
				String suffix = subtheory.substring(delim + delimiter.length());
				return getMakeSubspace(suffix, make);
			}
			return this;
		}
		NameSpace ns;
		if (containsKey(prefix)) {
			ns = get(prefix);
		} else if (make) {
			ns = new NameSpace(prefix.intern(), this);
			put(prefix, ns);
		} else {
			return null;
		}
		setKeyword(ProverSyntax.TYPE_THEORY, prefix);
		if (hasdelim) {
			String suffix = subtheory.substring(delim + delimiter.length());
			return ns.getMakeSubspace(suffix, make);
		}
		return ns;
	}

	/**
	 * Adds a list of keywords of a given type. Keywords may be qualified names
	 * with paths separated by namespace delimiter, if the qualified flag is
	 * set. Creates new sub-namespaces where necessary.
	 * 
	 * @param type
	 * @param words
	 * @param qualified
	 */
	public void setKeywords(String type, String[] words, boolean qualified) {
		assert type != null : "Illegal argument: type must be non-null";
		synchronized (keywords) {
			if (type.equals(ProverSyntax.TYPE_THEORY)) {
				keywords.remove(ProverSyntax.TYPE_THEORY);
				for (String subtheory : words) {
					// FIXME: this always behaves as qualified
					getSubspace(subtheory); // creates subtheories & keywords
				}
			} else {
				keywords.remove(type);
				for (String word : words) {
					if (!qualified && word.lastIndexOf(delimiter) != -1) {
						setKeyword(type, word);
						int delim = word.lastIndexOf(delimiter);
						String subtheory = word.substring(0, delim);
						String subword = word.substring(delim + delimiter.length());
						NameSpace ns = getSubspace(subtheory);
						ns.setKeyword(type, subword);
					}
				}
			}
		}
		notifyChange();
	}

	/**
	 * Syntax has changed - generate an event.
	 */
	void notifyChange() {
		// TODO: reinstate this
		// syntax.firePGIPEvent(new InternalEvent(this,"",null));
	}
}