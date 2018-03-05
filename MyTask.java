import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;

/**
 * 
 * @author Maftei Stefan - Radu
 *
 */
// This class implements the tasks/jobs for the database's workers; this job is
// to find the index of elements that match a given condition
public class MyTask implements Runnable {
	private Column column; // table's column to check
	private int start, stop; // search limits (values of column are distributed equally between workers)
	private ArrayList<Integer> indices; // list of result indices
	private String comparator; // condition comparator
	private String value; // condition value
	private CyclicBarrier barrier; // synchronization between workers and database

	public MyTask(CyclicBarrier barrier_in, Column column_in, int step_in, String comp_in, String value_in,
			ArrayList<Integer> indices_in, int numWorkers_in, ExecutorService worker) {
		this.column = column_in;
		this.comparator = comp_in;
		this.value = value_in;
		this.indices = indices_in;
		this.start = (int) ((step_in - 1) * ((float) ((float) column.getValues().size()) / ((float) numWorkers_in)));
		this.stop = (int) (step_in * ((float) ((float) column.getValues().size()) / ((float) numWorkers_in)));
		this.barrier = barrier_in;
	}

	// worker action
	// every worker checks the given condition by verifying a certain interval of
	// column values; these intervals are equally distributed between workers, so it
	// will be computed in parallel; the results are collected in database
	@Override
	public void run() {
		if (comparator.equals("<")) {
			if (column.getType().equals("int")) {
				for (int i = start; i < stop; i++) {
					if (((int) column.getValues().get(i)) < Integer.parseInt(value)) {
						indices.add(i);
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else if (comparator.equals(">")) {
			if (column.getType().equals("int")) {
				for (int i = start; i < stop; i++) {
					if (((int) column.getValues().get(i)) > Integer.parseInt(value)) {
						indices.add(i);
					}
				}
			} else {
				throw new RuntimeException();
			}
		} else {
			if (column.getType().equals("int")) {
				for (int i = start; i < stop; i++) {
					if (((int) column.getValues().get(i)) == Integer.parseInt(value)) {
						indices.add(i);
					}
				}
			} else if (column.getType().equals("string")) {
				for (int i = start; i < stop; i++) {
					if (column.getValues().get(i).equals(value)) {
						indices.add(i);
					}
				}
			} else {
				for (int i = start; i < stop; i++) {
					if (((boolean) column.getValues().get(i))) {
						if (value.equals("true")) {
							indices.add(i);
						}
					} else {
						if (value.equals("false")) {
							indices.add(i);
						}
					}
				}
			}
		}

		// synchronize workers and database (flow continues after workers have finished)
		try {
			barrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		} catch (BrokenBarrierException e) {
			throw new RuntimeException();
		}
	}

}
