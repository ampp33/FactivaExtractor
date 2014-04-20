package org.malibu.msu.factiva.extractor;

public class FactivaQuery {
	private int id;
	private String searchString;
	private String dateRangeFrom;
	private String dateRangeTo;
	private String companyName;
	private String[] sources;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getSearchString() {
		return searchString;
	}
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
	public String getDateRangeFrom() {
		return dateRangeFrom;
	}
	public void setDateRangeFrom(String dateRangeFrom) {
		this.dateRangeFrom = dateRangeFrom;
	}
	public String getDateRangeTo() {
		return dateRangeTo;
	}
	public void setDateRangeTo(String dateRangeTo) {
		this.dateRangeTo = dateRangeTo;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	public String[] getSources() {
		return sources;
	}
	public void setSources(String[] sources) {
		this.sources = sources;
	}
}
