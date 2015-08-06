package org.mskcc.oncotree.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.util.List;
import java.util.Properties;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Created by zhangh2 on 7/14/15.
 */
public class TumorType extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException{

		String propFileName = "properties/config.properties";
		Properties properties = new Properties();
		InputStream inputStream = TumorType.class.getClassLoader().getResourceAsStream(propFileName);

		if (inputStream != null) {
			properties.load(inputStream);
		} else {
			throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
		}
		inputStream.close();

		String tumorTypeFilePath = properties.getProperty("tumor_type_file_path");



		response.setContentType("text/plain");

		// print some html
		ServletOutputStream out = response.getOutputStream();

		// print the file
		InputStream in = null;
		try {
			in = new BufferedInputStream
					(new FileInputStream(tumorTypeFilePath) );
			int ch;
			while ((ch = in.read()) !=-1) {
				out.print((char)ch);
			}
		}
		finally {
			if (in != null) in.close();  // very important
		}
	}
}
