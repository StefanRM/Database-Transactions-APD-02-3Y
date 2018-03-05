import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * @author Maftei Stefan - Radu
 *
 */
// This class implements a database according to the given interface
public class Database implements MyDatabase {
	private Map<String, Table> tables; // all database's tables
	private ExecutorService worker; // database's workers
	private int numWorkerThreads; // number of workers that database can perform

	// convert certain Java Types into Database Types
	private String convertToDatabaseType(Object object) {
		if (object instanceof String) {
			return "string";
		}

		if (object instanceof Integer) {
			return "int";
		}

		return "bool";
	}

	// find all database's entries that satisfy a given condition
	synchronized private ArrayList<Integer> getIndexOfConditionSatisfaction(Table t, String condition) {
		// extract condition parameters
		String[] tokens = condition.split(" ");
		String columnName = tokens[0];
		String comparator = tokens[1];
		String value = tokens[2];

		// find the column from condition
		Column column = null;
		for (Column col : t.getColumns()) {
			if (col.getName().equals(columnName)) {
				column = col;
			}
		}

		if (column == null) {
			throw new RuntimeException();
		}

		ArrayList<Integer> indices = new ArrayList<Integer>(); // list of result indices
		ArrayList<ArrayList<Integer>> listIndicesWorkers = new ArrayList<ArrayList<Integer>>(); // list of indices lists
																								// resulted from
																								// database's workers

		CyclicBarrier barrier = new CyclicBarrier(numWorkerThreads + 1); // barrier for database and its workers
																			// synchronization

		for (int i = 1; i <= this.numWorkerThreads; i++) { // iterate through workers
			listIndicesWorkers.add(i - 1, new ArrayList<Integer>()); // initialize worker list

			// assign a job to the worker
			worker.submit(new MyTask(barrier, column, i, comparator, value, listIndicesWorkers.get(i - 1),
					numWorkerThreads, worker));
		}

		// wait for workers to be done
		try {
			barrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		} catch (BrokenBarrierException e) {
			throw new RuntimeException();
		}

		// assemble list of result indices
		for (int i = 1; i <= this.numWorkerThreads; i++) {
			indices.addAll(listIndicesWorkers.get(i - 1));
		}

		return indices;
	}

	// compute values given by the select() function
	private Object resultSelectCalculations(String operation, Column column, ArrayList<Integer> indices) {
		// if the computation must be made for all column values
		if (indices == null) {
			indices = new ArrayList<Integer>();
			for (int i = 0; i < column.getValues().size(); i++) {
				indices.add(i);
			}
		}

		switch (operation) { // do the operation
		case "min":
			if (!column.getType().equals("int")) {
				throw new RuntimeException();
			} else {
				int min = Integer.MAX_VALUE;
				for (int i = 0; i < indices.size(); i++) {
					if (((int) column.getValues().get(indices.get(i))) < min) {
						min = ((int) column.getValues().get(indices.get(i)));
					}
				}

				return min;
			}

		case "max":
			if (!column.getType().equals("int")) {
				throw new RuntimeException();
			} else {
				int max = Integer.MIN_VALUE;
				for (int i = 0; i < indices.size(); i++) {
					if (((int) column.getValues().get(indices.get(i))) > max) {
						max = ((int) column.getValues().get(indices.get(i)));
					}
				}

				return max;
			}

		case "sum":
			if (!column.getType().equals("int")) {
				throw new RuntimeException();
			} else {
				int sum = 0;
				for (int i = 0; i < indices.size(); i++) {
					sum += ((int) column.getValues().get(indices.get(i)));
				}

				return sum;
			}

		case "avg":
			if (!column.getType().equals("int")) {
				throw new RuntimeException();
			} else {
				int sum = 0;
				for (int i = 0; i < indices.size(); i++) {
					sum += ((int) column.getValues().get(indices.get(i)));
				}

				return sum / indices.size();
			}

		case "count":
			return indices.size();
		}

		return null;
	}

	public Database() {
		this.tables = new HashMap<String, Table>();
	}

	// DataBase initialized; create workers
	@Override
	public void initDb(int numWorkerThreads) {
		this.numWorkerThreads = numWorkerThreads;
		worker = Executors.newFixedThreadPool(numWorkerThreads);
	}

	// DataBase stopped
	// stop workers
	@Override
	public void stopDb() {
		worker.shutdown();
	}

	@Override
	public void createTable(String tableName, String[] columnNames, String[] columnTypes) {
		tables.put(tableName, new Table(tableName, columnNames, columnTypes));
	}

	// it is a reader process in the readers-writers paradigm
	@Override
	public ArrayList<ArrayList<Object>> select(String tableName, String[] operations, String condition) {
		Table t = this.tables.get(tableName);

		// if a transaction is occurring then only the thread that initialized it can
		// continue
		while (t.getTransactionSem().isLocked() && !t.getTransactionSem().isHeldByCurrentThread())
			;

		// apply readers-writers paradigm
		try {
			t.getMutexR().acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}
		int nr = t.getNr();
		nr = nr + 1;
		t.setNr(nr);
		if (nr == 1) {
			try {
				t.getRw().acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException();
			}
		}
		t.getMutexR().release();

		// select() execution
		ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
		String op = "";
		String columnName = "";

		// apply for every operation in the operation list
		for (int i = 0; i < operations.length; i++) {
			ArrayList<Object> entityToAdd = new ArrayList<Object>();

			// get operation
			if (operations[i].contains("(")) {
				op = operations[i].substring(0, operations[i].indexOf("("));
				columnName = operations[i].substring(operations[i].indexOf("(") + 1, operations[i].indexOf(")"));
			} else {
				op = "-"; // default operation
				columnName = operations[i];
			}

			// get column's name
			Column column = null;
			for (Column col : t.getColumns()) {
				if (col.getName().equals(columnName)) {
					column = col;
				}
			}

			// verify condition and extract list of indices
			if (condition.equals("")) {
				if (op.equals("-")) {
					entityToAdd.addAll(column.getValues());
				} else {
					entityToAdd.add(resultSelectCalculations(op, column, null));
				}
			} else {
				ArrayList<Integer> indices = getIndexOfConditionSatisfaction(t, condition);
				if (op.equals("-")) {
					for (Integer index : indices) {
						entityToAdd.add(column.getValues().get(index));
					}
				} else {
					entityToAdd.add(resultSelectCalculations(op, column, indices));
				}
			}

			result.add(entityToAdd); // add to the computed list of select()
		}

		// apply readers-writers paradigm
		try {
			t.getMutexR().acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}

		nr = nr - 1;
		t.setNr(nr);
		if (nr == 0) {
			t.getRw().release();
		}
		t.getMutexR().release();

		return result;
	}

	// it is a writer process in the readers-writers paradigm
	@Override
	public void update(String tableName, ArrayList<Object> values, String condition) {
		Table t = this.tables.get(tableName);

		// if a transaction is occurring then only the thread that initialized it can
		// continue
		while (t.getTransactionSem().isLocked() && !t.getTransactionSem().isHeldByCurrentThread())
			;

		// apply readers-writers paradigm
		try {
			t.getRw().acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}

		// update() execution
		if (condition.equals("")) { // verify condition and update elements at position given from indices list
			int sizeOfAColumn = t.getColumns().get(0).getValues().size();
			for (int i = 0; i < values.size(); i++) {
				for (int j = 0; j < sizeOfAColumn; j++)
					t.getColumns().get(i).getValues().set(j, values.get(i));
			}
		} else {
			for (Integer index : getIndexOfConditionSatisfaction(t, condition)) {
				for (int i = 0; i < values.size(); i++) {
					t.getColumns().get(i).getValues().set(index, values.get(i));
				}
			}
		}

		// apply readers-writers paradigm
		t.getRw().release();
	}

	// it is a writer process in the readers-writers paradigm
	@Override
	public void insert(String tableName, ArrayList<Object> values) {
		Table t = this.tables.get(tableName);

		// if a transaction is occurring then only the thread that initialized it can
		// continue
		while (t.getTransactionSem().isLocked() && !t.getTransactionSem().isHeldByCurrentThread())
			;

		// apply readers-writers paradigm
		try {
			t.getRw().acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}

		// insert() execution
		List<Column> columnsInTable = t.getColumns();

		for (int i = 0; i < values.size(); i++) {
			if (convertToDatabaseType(values.get(i)).equals(columnsInTable.get(i).getType())) { // check type
				columnsInTable.get(i).getValues().add(values.get(i)); // add in table
			} else { // wrong values input
				for (int j = 0; j < i; j++) { // eliminate values added on the previous columns
					columnsInTable.get(j).getValues().remove(columnsInTable.get(j).getValues().size() - 1);
				}
				throw new RuntimeException();
			}
		}

		// apply readers-writers paradigm
		t.getRw().release();
	}

	@Override
	public void startTransaction(String tableName) {
		this.tables.get(tableName).getTransactionSem().lock(); // a thread started a transaction by locking the table
	}

	@Override
	public void endTransaction(String tableName) {
		this.tables.get(tableName).getTransactionSem().unlock(); // a thread ended a transaction by unlocking the table
	}

}
