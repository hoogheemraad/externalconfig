package plugin;

import java.io.*;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.net.*;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.IO;

public class PropertiesFileConfigurationPlugin extends PlayPlugin {
	
	@Override
	public void onConfigurationRead() {
		String[] propertiesFilenames = Play.configuration.getProperty("externalConfig.fileName", "/" + Play.id + ".properties").split(",");
		
		for (String propertiesFilename : propertiesFilenames) {
			Logger.info("Loading configuration from " + propertiesFilename);

            Properties properties = null;
            try {
                properties = loadPropertiesFromAppropriateSource(propertiesFilename);
            } catch (IOException e) {
                Logger.error(e, "Unable to load properties from file: " + propertiesFilename);
            }


            for (Entry<Object, Object> entry : properties.entrySet()) {
				Play.configuration.setProperty((String) entry.getKey(),(String) entry.getValue());
			}
		}
	}

    /**
     * Determines if the given String represents a potentially valid URL.
     * @param value String value to check and see if it represents a URL.
     * @return whether the given String represents a potentially valid URL.
     */
    private static boolean isUrl(String value){
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    /**
     * Loads a new Properties object from the given String argument which may point to a file on the Classpath or,
     * alternatively a URL.
     * @param propertiesFilename String referencing the properties file to be loaded.
     * @return Properties oject populated by the given propertiesFilename.
     */
    private Properties loadPropertiesFromAppropriateSource(String propertiesFilename) throws IOException{
        Properties properties = null;
        if(isUrl(propertiesFilename)){
            properties = new Properties();

            URL url = new URL(propertiesFilename);
            url.openConnection();

            properties.load(url.openStream());
        } else {
            properties = IO.readUtf8Properties(this.getClass().getResourceAsStream(propertiesFilename));
        }

        return properties;
    }

}
