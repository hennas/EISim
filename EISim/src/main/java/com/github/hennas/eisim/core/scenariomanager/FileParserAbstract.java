package com.github.hennas.eisim.core.scenariomanager;

public abstract class FileParserAbstract {
	protected String file;

	protected FileParserAbstract(String file) {
		this.file = file;
	}

	public abstract boolean parse();
}
