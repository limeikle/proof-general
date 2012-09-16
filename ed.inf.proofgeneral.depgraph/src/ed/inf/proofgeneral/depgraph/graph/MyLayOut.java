package ed.inf.proofgeneral.depgraph.graph;

import java.util.ArrayList;
import java.util.Iterator;

public class MyLayOut {
	private final ArrayList<Level> levels = new ArrayList<Level>();

	public MyLayOut() {
		levels.add(new Level(0));

	}

	public void addNode(MyNode node) {
		final int level = adjustToParents(node);
		getLevel(level).add(node);

		int biggestLevel = 0;
		final Iterator<Level> it = levels.iterator();
		while (it.hasNext()) {
			final Level curLevel = it.next();
			if (curLevel.getNodes().size() > getLevel(biggestLevel).getNodes()
					.size()) {
				biggestLevel = curLevel.getNumber();
			}
		}
		centralise(biggestLevel);
	}

	private void centralise(int biggestLevel) {
		final int adj = getLevel(biggestLevel).getNodes().size();
		final Iterator<Level> it = levels.iterator();
		while (it.hasNext()) {
			final Level current = it.next();
			final int diff = (adj - current.getNodes().size()) * 100;
			current.setOffset(diff);

		}

	}

	public int adjustToParents(MyNode node) {
		final ArrayList<MyNode> parents = node.parents;
		final Iterator<MyNode> it = parents.iterator();
		int level = 1;
		while (it.hasNext()) {
			final MyNode parent = it.next();
			if (parent.getLevel() >= level) {
				level = parent.getLevel() + 1;
			}

		}
		return level;

	}

	public void addOldNode(MyNode node) {
		ArrayList<MyNode> children;
		final int level = adjustToParents(node);
		if (level > node.getLevel()) {
			getLevel(node.getLevel()).delete(node);
			getLevel(level).add(node);
			children = node.children;
			final Iterator<MyNode> it = children.iterator();
			while (it.hasNext()) {
				addOldNode(it.next());
			}

		}

	}

	public Level getLevel(int level) {
		while (levels.size() <= level) {
			final Level newLevel = new Level(levels.size());
			levels.add(newLevel);

		}
		return levels.get(level);

	}

	public int size() {

		return levels.size();
	}

	public ArrayList<MyNode> getLevelNodes(int n) {
		return getLevel(n).getNodes();
	}

	public void arrange() {
		final Iterator<Level> it = levels.iterator();
		int maxLevelSize = 0;
		// int maxLevel=0;
		while (it.hasNext()) {
			final Level level = it.next();
			if (maxLevelSize < level.getSize()) {
				maxLevelSize = level.getSize();
				// maxLevel=level.getNumber();
			}
		}
		final Iterator<Level> itr = levels.iterator();
		while (itr.hasNext()) {
			final Level level = itr.next();
			level.shift((maxLevelSize - level.getSize()) / 2);
			level.setMaxSize(maxLevelSize);
			level.adaptLevel();
		}

	}

}
