import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author Maftei Stefan - Radu
 *
 */
// This class implements a database's table that works according to
// readers-writers paradigm
public class Table {
	private String name;
	private List<Column> columns;
	private Semaphore mutexR, rw; // readers-writers paradigm semaphores
	private int nr = 0; // readers-writers paradigm tracker for readers
	private ReentrantLock transactionSem; // table lock for transaction

	public Table(String tableName, String[] columnNames, String[] columnTypes) {
		this.name = tableName;
		this.columns = new ArrayList<Column>();

		for (int i = 0; i < columnNames.length; i++) {
			this.columns.add(new Column(columnNames[i], columnTypes[i]));
		}

		mutexR = new Semaphore(1);
		rw = new Semaphore(1);
		nr = 0;
		transactionSem = new ReentrantLock();
	}

	public int getNr() {
		return nr;
	}

	public void setNr(int nr) {
		this.nr = nr;
	}

	public Semaphore getMutexR() {
		return mutexR;
	}

	public Semaphore getRw() {
		return rw;
	}

	public ReentrantLock getTransactionSem() {
		return transactionSem;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	@Override
	public String toString() {
		String toDisp = this.name + "\n";

		for (Column entry : this.columns) {
			toDisp += entry.getName() + " -> " + entry.getType() + ": ";
			for (Object object : entry.getValues()) {
				toDisp += object + " ";
			}
			toDisp += "\n";
		}
		return toDisp;
	}
}
