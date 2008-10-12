import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;


public class Solutions {
	private LinkedList<Solution> list = new LinkedList<Solution>();

	public void insertCacheFresh() {
		if (list.size() == 0) {
			list.addFirst(new Solution(1, Cache.cacheAccessTime));
		} else {
			 ListIterator<Solution> itr = list.listIterator();
			 while (itr.hasNext()) {
				 Solution s1 = itr.next();
				 s1.AddFresh();

			 }
		}
	}

	public void insert(Solution s) {
		if (list.size() == 0) {
			list.addFirst(s);
		} else {
			insert(list, s);
		}
	}

    private static void insert(LinkedList<Solution> l, Solution s) {
		 ListIterator<Solution> itr = l.listIterator();
		 while (itr.hasNext()) {
			 Solution s1 = itr.next();
			 s1.addSolution(s);

		 }

    }

    @SuppressWarnings("unchecked")
	private static LinkedList<Solution> deepCopy(final LinkedList<Solution> s) {
    	LinkedList<Solution> n = (LinkedList<Solution>) s.clone();
		 ListIterator<Solution> itr = n.listIterator();
		 while (itr.hasNext()) {
			 Solution s1 = itr.next();
			 itr.set(s1.clone());

		 }
    	return n;
    }

	public void insert(ArrayList<Solution> s) {
		if (list.size() == 0) {
			list.addAll(s);

		} else {
			ArrayList<LinkedList<Solution>> ls = new ArrayList<LinkedList<Solution>>(s.size());
			ls.add(list);
			for (int i = 1; i<ls.size(); ++i) {
				ls.add(deepCopy(list));
			}

			for (int i = 0; i<ls.size(); ++i) {
				insert(ls.get(i), s.get(i));
			}

			for (int i = 1; i<ls.size(); ++i) {
				list.addAll(ls.get(i));
			}

		}
	}
	public double pay(User u, int datalen, Cache c) {

		double max = 0;
		Solution s = null;
		 ListIterator<Solution> itr = list.listIterator();
		 while (itr.hasNext()) {
			 Solution s1 = itr.next();
			 double value = s1.tryPay(u, datalen);
			 if (value > max) {
				 max = value;
				 s = s1;
			 }
		 }
		// apply this solution's change
		 if (s!= null) {
			 s.pay(u, datalen);
			 s.apply(c);
		 } else {
			 throw new RuntimeException();
		 }
		return max;
	}
}
