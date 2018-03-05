import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Maftei Stefan - Radu
 *
 */
// This class implements a database's column
public class Column {
	private String name;
	private String type; // type of column values
	private List<Object> values; // column values

	public Column(String columnName, String columnType) {
		this.name = columnName;
		this.type = columnType;
		this.values = new ArrayList<Object>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Object> getValues() {
		return values;
	}

	public void setValues(List<Object> values) {
		this.values = values;
	}
}
