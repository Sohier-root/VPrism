package com.stilog.analysevpi.utils;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class SystemInfo {
	public static final String VERSION = "VERSION";
	public static final String RELEASE_DATE = "RELEASE_DATE";
	public static final String JAVA_VERSION = "JAVA";
	public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private String version;
	private LocalDate releaseDate;
	private String javaVersion;
	
	private static SystemInfo instance;
	
	public static SystemInfo getInstance() {
		if(instance == null) {
			instance = new SystemInfo();
		}
		
		return instance;
	}
	
	private SystemInfo() {
		try {
			Properties prop = parseInfoFile();
			this.version = prop.getProperty(SystemInfo.VERSION);
			this.releaseDate = LocalDate.parse(prop.getProperty(SystemInfo.RELEASE_DATE), dateFormatter);
			this.javaVersion = prop.getProperty(SystemInfo.JAVA_VERSION);
		}
		catch(Exception e) {
			this.version = this.version == null ? "0.0.0" : this.version;
			this.releaseDate = this.releaseDate == null ? LocalDate.now() : this.releaseDate;
			this.javaVersion = this.javaVersion == null ? "Java17" : this.javaVersion;
		}
	}
	
	private Properties parseInfoFile() {
		Properties prop = new Properties();
		try {
			InputStream inputStream = getClass().getResourceAsStream("/com/stilog/analysevpi/About.txt");
			prop.load(inputStream);
		
		}
		catch(Exception e) {
			System.out.println("Erreur dans la lecture du fichier About");
			e.printStackTrace();
		}
		
		return prop;
	}

	/*
	 * GETTER & SETTER
	 */
	
	public String getVersion() {
		return version;
	}

	public LocalDate getReleaseDate() {
		return releaseDate;
	}

	public String getJavaVersion() {
		return javaVersion;
	}
	
}
