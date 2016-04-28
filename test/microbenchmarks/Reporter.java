package microbenchmarks;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;

//import dnl.utils.text.table.TextTable;

public class Reporter {
	private String[] columnNames;
	private List<Object[]> data;
	private boolean enableRowNumbering;
	private boolean isFirst = true;

	public Reporter() {
		data = new LinkedList<Object[]>();
		setEnableRowNumbering(true);
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
	}

	public void collect(Object... objects) {
		data.add(objects);
		if(isFirst) {
			for (int i = 0; i < columnNames.length; i++) {
				String string = columnNames[i];
				System.out.print(string + "\t");
			}
			System.out.println("");
			for (int i = 0; i < columnNames.length * 8; i++) {
				System.out.print("=");
			}
			System.out.println("");
			isFirst = false;
		}
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			System.out.print(object.toString() + "\t");
		}
		System.out.println("");
	}

	public void print() {
		// Object[][] newData = new Object[data.size()][columnNames.length];
		// for (int i = 0; i < newData.length; i++) {
		// newData[i] = data.get(i);
		// }
		// TextTable tt = new TextTable(columnNames, newData);
		// tt.setAddRowNumbering(isEnableRowNumbering());
		// tt.printTable();
		if (data.size() == 0)
			return;
		for (int i = 0; i < columnNames.length; i++) {
			String string = columnNames[i];
			System.out.print(string + "\t");
		}
		System.out.println("");
		for (int i = 0; i < columnNames.length * 8; i++) {
			System.out.print("=");
		}
		System.out.println("");
		for (Iterator<Object[]> iterator = data.iterator(); iterator.hasNext();) {
			Object[] objects = iterator.next();
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				System.out.print(object.toString() + "\t");
			}
			System.out.println("");
		}
	}

	public boolean isEnableRowNumbering() {
		return enableRowNumbering;
	}

	public void setEnableRowNumbering(boolean enableRowNumbering) {
		this.enableRowNumbering = enableRowNumbering;
	}
}
