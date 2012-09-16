package ed.inf.proofgeneral.depgraph.graph;

import java.util.ArrayList;
import java.util.Iterator;

public class Level {
	private static int maxX = -1;

	private final int number;

	private final ArrayList<MyNode> level = new ArrayList<MyNode>();

	private int offset = -1;

	private int dim;

	public Level(int num) {
		number = num;

	}

	private int maxSize;

	public void add(MyNode node) {

		level.add(node);
		node.setLevel(number);
		node.setY((number - 1) * 100);
		arrangeLevel();
		System.out.println("node: " + node.getName() + " level: " + number
				+ " x: " + node.getX() + " y: " + number * 100);

	}

	public void delete(MyNode node) {
		level.remove(node);
		arrangeLevel();
	}

	public void arrangeLevel() {
		final Iterator<MyNode> it = level.iterator();
		int x = 1;
		while (it.hasNext()) {
			final MyNode curNode = it.next();
			curNode.setX(x + offset);
			if (curNode.getSize() != 0) {
				x += curNode.getSize() + 30;
			} else {
				x += 200;
				// System.out.println("size is zero: "+curNode.getName());
			}
		}

	}

	public ArrayList<MyNode> getNodes() {
		return level;
	}

	public void setOffset(int i) {
		offset = i;
		final Iterator<MyNode> it = level.iterator();
		int x = 1;
		while (it.hasNext()) {
			final MyNode curNode = it.next();
			curNode.setX(x + i);
			x += 200;
		}

	}

	public int getNumber() {

		return number;
	}

	public int getSize() {
		final ArrayList<MyNode> nodes = getNodes();
		int size = 0;
		for (int i = 0; i < nodes.size(); i++) {
			size += (nodes.get(i)).getSize();
		}
		size += 30 * (nodes.size() - 1);
		return size;

	}

	public void shift(int i) {
		this.offset = i;

	}

	public void adaptLevel() {
		final ArrayList<MyNode> nodes = getNodes();
		final int space = 30 + ((maxSize - getSize()) / (nodes.size() + 1));
		if (nodes.size() > 0) {
			MyNode nodeOld = nodes.get(0);
			nodeOld.setX(space);
			// int size=0;
			for (int i = 1; i < nodes.size(); i++) {
				final MyNode nod = nodes.get(i);
				nod.setX(nodeOld.getX() + nodeOld.getSize() + space);

				// int sec=maxSize/nodes.size();
				// nod.setX(i*sec+((sec-nod.getSize())/2));
				nodeOld = nod;
			}
		}
	}

	public void setMaxSize(int maxLevelSize) {
		maxSize = maxLevelSize;

	}

}
