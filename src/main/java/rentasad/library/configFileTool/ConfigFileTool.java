package rentasad.library.configFileTool;

import lombok.extern.java.Log;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.logging.Level;

/**
 * Gustini GmbH (2025)
 * Creation: 18.02.2015
 * Rentasad Library
 * rentasad.lib.tools.configFileTool
 *
 * @author Matthias Staud
 * <p>
 * <p>
 * Description:
 * <p>
 * Tool zum Erstellen und Auslesen von INI-Dateien
 */
@Log
public class ConfigFileTool extends AbstractLoggingListener
{
	private static final char[] PASSWORD = "48joVKpQ+jIkzy-oW!0A".toCharArray();
	private static final byte[] SALT = { (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, };


	private ConfigFileTool()
	{
		super();
	}

	/**
	 * Writes the configuration to an INI file with the specified section and key-value pairs.
	 *
	 * @param fileName The name of the file where the configuration should be written.
	 * @param sektionString The name of the section to be added in the INI file.
	 * @param configMap A map containing key-value pairs to be added under the specified section.
	 * @throws IOException If an I/O error occurs during writing to the file.
	 */
	public static void writeConfiguration(String fileName, String sektionString, Map<String, String> configMap) throws IOException
	{
		INIConfiguration ini = new INIConfiguration();

		for (Map.Entry<String, String> entry : configMap.entrySet())
		{
			ini.setProperty(sektionString + "." + entry.getKey(), entry.getValue());
		}

		try (FileWriter fileWriter = new FileWriter(fileName))
		{
			ini.write(fileWriter);
		} catch (ConfigurationException e)
		{
			throw new IOException("Fehler beim Schreiben der INI-Datei: " + e.getMessage(), e);
		}
	}

	/**
	 * Reads the configuration from an INI file and returns the settings found in the specified section.
	 *
	 * @param fileName The name of the file from which to read the configuration.
	 * @param sektionString The section within the INI file to read.
	 * @return A map of configuration keys and values from the specified section.
	 * @throws IOException If there is an error reading the file.
	 * @throws ConfigFileToolException If the specified section is not found in the configuration file.
	 */
	public static Map<String, String> readConfiguration(String fileName, String sektionString) throws IOException, ConfigFileToolException
	{
		File configFile = new File(fileName);
		if (configFile.exists())
		{
			String message = String.format("Section %s of ConfigFile %s loaded.", sektionString, configFile.getAbsolutePath());
			logMessage(message, java.util.logging.Level.FINER);

			try (FileReader fileReader = new FileReader(configFile))
			{
				return readConfigurationFromReader(fileReader, sektionString);
			} catch (ConfigurationException e)
			{
				throw new IOException("Fehler beim Lesen der INI-Datei: " + e.getMessage(), e);
			}
		}
		else
		{
			throw new FileNotFoundException(configFile.getAbsolutePath());
		}
	}

	/**
	 * Reads the configuration from a resource file.
	 *
	 * @param fileName      The name of the resource file.
	 * @param sektionString The section of the variables in the INI file.
	 * @return A map containing the keys and values of the variables.
	 * @throws IOException             If an I/O error occurs while reading the file.
	 * @throws ConfigFileToolException If the specified file is not found or the section is not found in the file.
	 */
	public static Map<String, String> readConfigurationFromResources(final String fileName, final String sektionString) throws IOException, ConfigFileToolException
	{
		try {
			URL resourceUrl = ConfigFileTool.class.getClassLoader().getResource(fileName);
			if (resourceUrl == null) {
				throw new IllegalArgumentException(fileName + " is not found in resources.");
			}

			try (InputStream inputStream = resourceUrl.openStream())
			{
				return readConfiguration(inputStream, sektionString);
			}
		} catch (IllegalArgumentException e) {
			log.severe("Die Konfigurationsdatei '" + fileName + "' konnte nicht im Klassenpfad gefunden werden. \r\nVersuchter Pfad: " + (ConfigFileTool.class.getClassLoader().getResource(fileName) != null ? ConfigFileTool.class.getClassLoader().getResource(fileName).toString() : "Nicht verfügbar"));
			throw e;
		}
	}

	/**
	 * Reads configuration from a specified section in an INI file provided as an InputStream.
	 *
	 * @param inputStream The input stream containing the INI file content.
	 * @param sektionString The section in the INI file to read configuration from.
	 * @return A map containing the key-value pairs of configuration settings from the specified section.
	 * @throws IOException If an I/O error occurs during reading the INI file.
	 * @throws ConfigFileToolException If the specified section is not found in the INI file.
	 */
	public static Map<String, String> readConfiguration(InputStream inputStream, String sektionString) throws IOException, ConfigFileToolException
	{
		try (InputStreamReader reader = new InputStreamReader(inputStream))
		{
			return readConfigurationFromReader(reader, sektionString);
		} catch (ConfigurationException e)
		{
			throw new IOException("Fehler beim Lesen der INI-Datei: " + e.getMessage(), e);
		}
	}

	/**
	 * Helper method to read configuration from a Reader.
	 */
	private static Map<String, String> readConfigurationFromReader(Reader reader, String sektionString) throws ConfigurationException, ConfigFileToolException, IOException
	{
		Map<String, String> configMap = new HashMap<>();
		INIConfiguration ini = new INIConfiguration();
		ini.read(reader);

		Set<String> sections = ini.getSections();
		if (!sections.contains(sektionString))
		{
			String message = "Folgende Section wurde in der INI-Datei nicht gefunden: " + sektionString;
			logMessage(message, Level.SEVERE);
			throw new ConfigFileToolException(message);
		}

		SubnodeConfiguration section = ini.getSection(sektionString);
		Iterator<String> keys = section.getKeys();
		while (keys.hasNext())
		{
			String key = keys.next();
			configMap.put(key, section.getString(key));
		}

		return configMap;
	}

	/**
	 * Reads all sections from an INI file and returns their configuration as a nested map.
	 *
	 * @param fileName The name of the INI file to be read.
	 * @return A map where each key is a section name and the value is another map containing key-value pairs of configurations within that section.
	 * @throws ConfigFileToolException If there is an issue reading the INI file or processing its content.
	 */
	public static Map<String, Map<String, String>> readIniFileWithAllSections(final String fileName) throws ConfigFileToolException
	{
		Map<String, Map<String, String>> sectionConfigMap = new HashMap<>();
		try
		{
			String[] sections = getSections(fileName);
			for (String sectionName : sections)
			{
				sectionConfigMap.put(sectionName, readConfiguration(fileName, sectionName));
			}
		} catch (IOException e)
		{
			throw new ConfigFileToolException(e);
		}
		return sectionConfigMap;
	}

	/**
	 * Reads an INI file from the resources and loads all sections and their key-value pairs.
	 *
	 * @param fileName The name of the INI file to read from the resources.
	 * @return A map where the key is the section name and the value is another map containing the key-value pairs within that section.
	 * @throws ConfigFileToolException If an I/O error occurs while reading the file.
	 */
	public static Map<String, Map<String, String>> readIniFileWithAllSectionsFromResources(final String fileName) throws ConfigFileToolException
	{
		Map<String, Map<String, String>> sectionConfigMap = new HashMap<>();
		try
		{
			String[] sections = getSectionsFromResources(fileName);
			for (String sectionName : sections)
			{
				sectionConfigMap.put(sectionName, readConfigurationFromResources(fileName, sectionName));
			}
		} catch (IOException e)
		{
			throw new ConfigFileToolException(e);
		}
		return sectionConfigMap;
	}

	/**
	 * Retrieves the sections from a configuration file.
	 *
	 * @param fileName the name of the configuration file to be read
	 * @return an array of section names found in the configuration file
	 * @throws IOException if an I/O error occurs
	 */
	public static String[] getSections(String fileName) throws IOException
	{
		File file = new File(fileName);
		System.out.println(file.getAbsolutePath());

		try (FileReader fileReader = new FileReader(file))
		{
			INIConfiguration ini = new INIConfiguration();
			ini.read(fileReader);
			Set<String> sections = ini.getSections();
			return sections.toArray(new String[0]);
		} catch (ConfigurationException e)
		{
			throw new IOException("Fehler beim Lesen der INI-Datei: " + e.getMessage(), e);
		}
	}

	/**
	 * Reads the sections from a specified configuration file.
	 *
	 * @param fileName The name of the configuration file to read sections from.
	 * @return An array containing the names of the sections found in the configuration file.
	 * @throws IOException If there is an error reading the file.
	 * @throws IllegalArgumentException If the file is not found.
	 */
	public static String[] getSectionsFromResources(String fileName) throws IOException
	{
		try (InputStream inputStream = ConfigFileTool.class.getClassLoader().getResourceAsStream(fileName))
		{
			if (inputStream == null)
			{
				throw new IllegalArgumentException(fileName + " is not found");
			}
			return getSectionsFromInputStream(inputStream);
		}
	}

	/**
	 * Helper method to get sections from an InputStream.
	 */
	private static String[] getSectionsFromInputStream(InputStream inputStream) throws IOException
	{
		try (InputStreamReader reader = new InputStreamReader(inputStream))
		{
			INIConfiguration ini = new INIConfiguration();
			ini.read(reader);
			Set<String> sections = ini.getSections();
			return sections.toArray(new String[0]);
		} catch (ConfigurationException e)
		{
			throw new IOException("Fehler beim Lesen der INI-Datei: " + e.getMessage(), e);
		}
	}

	/**
	 * Reads an INI file from the specified resource, and returns the sections as a Set of strings.
	 *
	 * @param fileName the name of the resource file to be read
	 * @return a Set of section names present in the INI file
	 * @throws IOException if an I/O error occurs when reading the file
	 * @throws IllegalArgumentException if the specified file is not found
	 */
	public static Set<String> getSectionsAsSetFromResource(String fileName) throws IOException
	{
		try (InputStream inputStream = ConfigFileTool.class.getClassLoader().getResourceAsStream(fileName))
		{
			if (inputStream == null)
			{
				throw new IllegalArgumentException(fileName + " is not found");
			}
			try (InputStreamReader reader = new InputStreamReader(inputStream))
			{
				INIConfiguration ini = new INIConfiguration();
				ini.read(reader);
				return ini.getSections();
			} catch (ConfigurationException e)
			{
				throw new IOException("Fehler beim Lesen der INI-Datei: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Description: Gibt verfuegbare Sektionen der INI-Datei zurueck
	 *
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static Set<String> getSectionsAsSet(String fileName) throws IOException
	{
		try (FileReader fileReader = new FileReader(new File(fileName)))
		{
			INIConfiguration ini = new INIConfiguration();
			ini.read(fileReader);
			return ini.getSections();
		} catch (ConfigurationException e)
		{
			throw new IOException("Fehler beim Lesen der INI-Datei: " + e.getMessage(), e);
		}
	}

	/**
	 * Encrypts a given property string using the PBEWithMD5AndDES algorithm.
	 *
	 * @param property The property string to be encrypted.
	 * @return The encrypted string, encoded in base64.
	 * @throws GeneralSecurityException If an error occurs during the encryption process.
	 * @throws UnsupportedEncodingException If the specified encoding (UTF-8) is unsupported.
	 */
	public static String encrypt(String property) throws GeneralSecurityException, UnsupportedEncodingException
	{
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
		return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
	}

	/**
	 * Encodes the provided byte array into a base64-encoded string.
	 *
	 * @param bytes The byte array to be encoded.
	 * @return A base64-encoded string representation of the input byte array.
	 */
	private static String base64Encode(byte[] bytes)
	{
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * Decrypts the given encrypted property string using a predefined password and salt.
	 *
	 * @param property The encrypted property string to be decrypted.
	 * @return The decrypted string.
	 * @throws GeneralSecurityException If a security exception occurs during the decryption process.
	 * @throws IOException If an I/O error occurs during the decryption process.
	 */
	public static String decrypt(String property) throws GeneralSecurityException, IOException
	{
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
		return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
	}

	/**
	 * Decodes a Base64 encoded string into a byte array.
	 *
	 * @param property The Base64 encoded string to decode.
	 * @return A byte array containing the decoded data.
	 * @throws IOException If an error occurs during decoding.
	 */
	private static byte[] base64Decode(String property) throws IOException
	{
		return Base64.getDecoder().decode(property);
	}
}
