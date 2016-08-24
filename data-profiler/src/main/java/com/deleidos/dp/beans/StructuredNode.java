package com.deleidos.dp.beans;

import java.util.ArrayList;
import java.util.List;

public class StructuredNode {
	private Integer id;
	private String field;
	private String path;
	private List<StructuredNode> children;
	
	public StructuredNode(String path, String field, Integer id) {
		setPath(path);
		setField(field);
		setId(id);
		setChildren(new ArrayList<StructuredNode>());
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public List<StructuredNode> getChildren() {
		return children;
	}
	public void setChildren(List<StructuredNode> children) {
		this.children = children;
	}
	
}
