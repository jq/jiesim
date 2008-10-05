package hm;

import java.util.*;

class test{

	public static void main(String[] args){
		testitem t1 = new testitem(1, 2);
	    testitem t2 = new testitem(2, 1);


		PriorityQueue<testitem> q1 = new PriorityQueue<testitem>(1,
	        new Comparator<testitem>() {
	          public int compare(testitem i, testitem j) {
	            int result = i.time1 -j.time1;
	            return result;
	          }
	        }
	    );

		PriorityQueue<testitem> q2 = new PriorityQueue<testitem>(1,
	        new Comparator<testitem>() {
	          public int compare(testitem i, testitem j) {
	            int result = i.time2 -j.time2;
	            return result;
	          }
	        }
	    );

	    q1.offer(t1);
	    q1.offer(t2);
	    q2.offer(t1);
	    q2.offer(t2);

	    System.out.println(q1.poll());
	    System.out.println(q1.poll());
	    System.out.println(q2.poll());
	    System.out.println(q2.poll());

	}

}
class testitem{
	public int time1;
	public int time2;

	public testitem(int t1, int t2){
		time1 = t1;
		time2 = t2;
	}

	public String toString(){
		return time1+"\t"+time2+"\n";
	}

}
