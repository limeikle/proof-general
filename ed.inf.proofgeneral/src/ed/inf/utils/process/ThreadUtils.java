/*
 *  $RCSfile: ThreadUtils.java,v $
 *
 *  Created on 01 Nov 2006 by gdutton
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;

/**
 * Thread-handling utility methods.
 */
public class ThreadUtils {

	/**
	 * The actual thread count returned by checking live threads
	 */
	public static int countEnumActiveThreads() {
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null) {
			tg = tg.getParent();
		}
		int count = tg.activeCount();
		Thread[] t = new Thread[((int) (1.1 * count))];
		tg.enumerate(t, true);
		int enumCount = 0;
		for (int i = 0; i < t.length; i++) {
			if (t[i] != null && t[i].isAlive()) {
				enumCount++;
			}
		}
		return enumCount;
	}

	/**
	 * The estimate thread count returned by ThreadGroup.activeCount.
	 * This is usually way too high.
	 */
	public static int countEstActiveThreads() {
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null) {
			tg = tg.getParent();
		}
		return tg.activeCount();
	}

	/**
	 * Sleeps for a specified number of seconds, without throwing an exception
	 * if interrupted.
	 * @param ms the number of ms to sleep
	 * @see Thread#sleep(long)
	 */
	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {}
	}

}
