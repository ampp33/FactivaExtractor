package org.malibu.msu.factiva.extractor.beans;

import java.util.Date;
import java.util.List;

public class FactivaQuery {
	private String id;
	private int queryRowNumber;
	private Date dateRangeFrom;
	private Date dateRangeTo;
	private String companyName;
	private List<String> sources;
	private List<String> subjects;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getQueryRowNumber() {
		return queryRowNumber;
	}
	public void setQueryRowNumber(int queryRowNumber) {
		this.queryRowNumber = queryRowNumber;
	}
	public Date getDateRangeFrom() {
		return dateRangeFrom;
	}
	public void setDateRangeFrom(Date dateRangeFrom) {
		this.dateRangeFrom = dateRangeFrom;
	}
	public Date getDateRangeTo() {
		return dateRangeTo;
	}
	public void setDateRangeTo(Date dateRangeTo) {
		this.dateRangeTo = dateRangeTo;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	public List<String> getSources() {
		return sources;
	}
	public void setSources(List<String> sources) {
		this.sources = sources;
	}
	public List<String> getSubjects() {
		return subjects;
	}
	public void setSubjects(List<String> subjects) {
		this.subjects = subjects;
	}
}
