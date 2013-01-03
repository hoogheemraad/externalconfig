package plugin;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.IO;

/**
 * This class takes care of reading additional properties from external sources.
 * @author Rugbyhead (Original)
 * @author Hoogheemraad
 * @author Neoh79
 * @author Rphutchinson
 */
public class PropertiesFileConfigurationPlugin extends PlayPlugin {
    /**
     * Key in the property files to denote the filename(s).
     */
    private static final String EC_FILENAME = "externalConfig.fileName";

    /**
     * Key in the property files to denote the absolute filename prefix for the
     * filename(s) mentioned in 'externalConfig.fileName'.
     */
    private static final String EC_FILE_ABSOLUTE_PATH = "externalConfig.fileAbsolutePath";

    /**
     * Key in the property files to denote the filename(s) using the absolute
     * file path(s).
     */
    private static final String EC_ABSOLUTE_FILENAME = "externalConfig.fileNameAbsolute";

    /**
     * Key in the property files to denote the filename(s) using the absolute
     * file path(s).
     */
    private static final String EC_URL = "externalConfig.URL";

    /**
     * The platform independent file separator.
     */
    private static final String SEPARATOR = System.getProperty("file.separator");

    /**
     * This method is automatically called when application.conf has been parsed
     * and loaded. It tries to read the properties mentioned in the
     * 'externalConfig.fileName' and the 'externalConfig.fileNameAbsolute'
     * properties.
     */
    public final void onConfigurationRead() {
        readPropertiesFromFileName();
        readPropertiesFromAbsolutePath();
        readPropertiesFromURL();
    }

    /**
     * Read 0, 1 or more files from files mentioned in the
     * 'externalConfig.fileName' property relative to the 'conf' directory. If
     * 'externalConfig.fileAbsolutePath' is present is uses that value to prefix
     * the filenames.
     */
    private void readPropertiesFromFileName() {
        String filenameValue = Play.configuration.getProperty(EC_FILENAME);
        String defaultFilename = "/" + Play.id + ".properties";
        String propertiesFileAbsolutePath = Play.configuration.getProperty(EC_FILE_ABSOLUTE_PATH);
        String propertiesFilenameAndPath;
        InputStream is;

        if (filenameValue == null || filenameValue.length() == 0) {
            play.Logger.debug(EC_FILENAME + " is empty so trying the default: " + defaultFilename);
            filenameValue = defaultFilename;
            return;
        }

        String[] propertiesFilenames = filenameValue.split(",");

        for (String propertiesFilename : propertiesFilenames) {
            propertiesFilenameAndPath = null;
            is = null;

            try {
                if (propertiesFileAbsolutePath != null && propertiesFileAbsolutePath.length() > 0) {

                    if (propertiesFilename.startsWith("/") || propertiesFilename.startsWith("/")) {
                        propertiesFilename = propertiesFilename.substring(1);
                    }
                    propertiesFilenameAndPath = propertiesFileAbsolutePath + SEPARATOR + propertiesFilename;
                    is = new FileInputStream(propertiesFilenameAndPath);
                } else {
                    propertiesFilenameAndPath = propertiesFilename;
                    is = this.getClass().getResourceAsStream(propertiesFilename);
                }

                if (is == null) {
                    Logger.warn("Configuration file '" + propertiesFilenameAndPath
                            + "' is not found. Ignoring the file.");
                } else {
                    Properties properties = IO.readUtf8Properties(is);
                    Logger.info("Loading configuration from " + propertiesFilenameAndPath);
                    readPropertySet(properties);
                }
            } catch (NullPointerException e) {
                Logger.error("Error when loading file " + propertiesFilenameAndPath + ". Check your '" + EC_FILENAME
                        + "' property.");
            } catch (RuntimeException e) {
                Logger.warn("Error when loading file " + propertiesFilenameAndPath + ". Ignoring the file.");
            } catch (FileNotFoundException e) {
                Logger.warn("Configuration file '" + propertiesFilenameAndPath + "' is not found. Ignoring the file.");
            }
        }
    }

    /**
     * Read 0, 1 or more files from files mentioned in the
     * 'externalConfig.fileNameAbsolute' property. This property should contain
     * comma separated absolute file paths.
     */
    private void readPropertiesFromAbsolutePath() {
        String absoluteFilenameValue = Play.configuration.getProperty(EC_ABSOLUTE_FILENAME);
        InputStream is;

        if (absoluteFilenameValue == null || absoluteFilenameValue.length() == 0) {
            play.Logger.debug(EC_ABSOLUTE_FILENAME + " is empty so ignoring this property");
            return;
        }
        String[] propertiesFilenames = absoluteFilenameValue.split(",");

        for (String propertiesFilename : propertiesFilenames) {
            is = null;

            try {
                is = new FileInputStream(propertiesFilename);
                Logger.warn("Configuration file '" + propertiesFilename
                        + "' (absolute path) is not found. Ignoring the file.");
                Properties properties = IO.readUtf8Properties(is);

                Logger.info("Loading configuration from " + propertiesFilename);
                readPropertySet(properties);

            } catch (NullPointerException e) {
                Logger.error("Error when loading file " + propertiesFilename + " (absolute path). Check your '"
                        + EC_FILENAME + "' property. Ignoring the value.");
            } catch (RuntimeException e) {
                Logger.error("Error when loading file " + propertiesFilename + " (absolute path). Ignoring the file.");
            } catch (FileNotFoundException e) {
                Logger.warn("Configuration file '" + propertiesFilename
                        + "' (absolute path) is not found. Ignoring the file.");
            }
        }
    }

    public void readPropertiesFromURL() {
        String urlValue = Play.configuration.getProperty(EC_URL);

        if (urlValue == null || urlValue.length() == 0) {
            play.Logger.debug(EC_URL + " is empty so ignoring this property");
            return;
        }
        String[] propertiesFilenames = urlValue.split(",");

        for (String propertiesFilename : propertiesFilenames) {
            Logger.info("Loading configuration from " + propertiesFilename);

            Properties properties = null;
            try {
                if (isUrl(propertiesFilename)) {
                    properties = new Properties();

                    URL url = new URL(propertiesFilename);
                    url.openConnection();

                    properties.load(url.openStream());
                }

                if (properties != null) {
                    readPropertySet(properties);
                } else {
                    Logger.error("Unable to load properties from URL: " + propertiesFilename + ". Ignoring this file.");
                }
            } catch (IOException e) {
                Logger.error("Unable to load file from URL: " + propertiesFilename + ". Ignoring this file.");
            }
        }
    }

    /**
     * Determines if the given String represents a potentially valid URL.
     * @param value
     *            String value to check and see if it represents a URL.
     * @return whether the given String represents a potentially valid URL.
     */
    private static boolean isUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    /**
     * This method actually set the properties into the Play.configuration. If
     * it encounters Play ID specific keys (denoted with a '%') it will try to
     * load these too. The idea comes from the
     * play.ant.PlayConfigurationLoadTask.java file of the Play 1.2.5 framework
     * files.
     * @param properties
     */
    private static void readPropertySet(Properties properties) {
        for (Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (key.startsWith("%")) {
                if (Play.id.length() > 0 && key.startsWith("%" + Play.id + ".")) {
                    key = key.substring(("%" + Play.id + ".").length());
                    Play.configuration.setProperty(key, value);
                }
            } else {
                Play.configuration.setProperty(key, value);
            }
        }
    }
}
