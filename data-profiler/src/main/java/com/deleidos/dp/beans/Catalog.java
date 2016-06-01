package com.deleidos.dp.beans;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class Catalog {
	private List<Schema> schemaCatalog;
	private List<DataSample> dataSamplesCatalog;
	
	@JsonProperty("schemaCatalog")
	public List<Schema> getSchemaCatalog() {
		return schemaCatalog;
	}
	
	public void setSchemaCatalog(List<Schema> schemaCatalog) {
		this.schemaCatalog = schemaCatalog;
	}
	
	@JsonProperty("dataSamplesCatalog")
	public List<DataSample> getDataSamplesCatalog() {
		return dataSamplesCatalog;
	}
	
	public void setDataSamplesCatalog(List<DataSample> dataSamplesCatalog) {
		this.dataSamplesCatalog = dataSamplesCatalog;
	}
	
}
