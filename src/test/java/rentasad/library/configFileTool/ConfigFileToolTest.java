package rentasad.library.configFileTool;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Umfassende Tests für die ConfigFileTool-Klasse.
 * Kompatibel mit Java 17.
 */
@DisplayName("ConfigFileTool Tests")
class ConfigFileToolTest
{
	private static final String RESOURCE_CONFIG_FILE = "config/test.ini";
	private static final String TEST_SECTION = "TEST";

	@TempDir
	Path tempDir;

	private Path tempIniFile;

	@BeforeEach
	void setUp()
	{
		tempIniFile = tempDir.resolve("test_config.ini");
	}

	// ==================== writeConfiguration Tests ====================

	@Nested
	@DisplayName("writeConfiguration Tests")
	class WriteConfigurationTests
	{
		@Test
		@DisplayName("Sollte eine INI-Datei mit einer Section und Key-Value-Paaren schreiben")
		void writeConfiguration_shouldWriteIniFileWithSectionAndKeyValues() throws IOException, ConfigFileToolException
		{
			// Arrange
			Map<String, String> configMap = new HashMap<>();
			configMap.put("host", "localhost");
			configMap.put("port", "8080");
			configMap.put("username", "admin");

			// Act
			ConfigFileTool.writeConfiguration(tempIniFile.toString(), "DATABASE", configMap);

			// Assert
			assertTrue(Files.exists(tempIniFile), "INI-Datei sollte erstellt worden sein");

			Map<String, String> readConfig = ConfigFileTool.readConfiguration(tempIniFile.toString(), "DATABASE");
			assertEquals("localhost", readConfig.get("host"));
			assertEquals("8080", readConfig.get("port"));
			assertEquals("admin", readConfig.get("username"));
		}

		@Test
		@DisplayName("Sollte eine leere Map schreiben können")
		void writeConfiguration_shouldHandleEmptyMap() throws IOException
		{
			// Arrange
			Map<String, String> emptyMap = new HashMap<>();

			// Act
			ConfigFileTool.writeConfiguration(tempIniFile.toString(), "EMPTY", emptyMap);

			// Assert
			assertTrue(Files.exists(tempIniFile), "INI-Datei sollte erstellt worden sein");
		}

		@Test
		@DisplayName("Sollte Sonderzeichen in Werten korrekt schreiben")
		void writeConfiguration_shouldHandleSpecialCharacters() throws IOException, ConfigFileToolException
		{
			// Arrange
			Map<String, String> configMap = new HashMap<>();
			configMap.put("path", "/usr/local/bin");
			configMap.put("message", "Hello World!");
			configMap.put("special", "äöüß");

			// Act
			ConfigFileTool.writeConfiguration(tempIniFile.toString(), "SPECIAL", configMap);

			// Assert
			Map<String, String> readConfig = ConfigFileTool.readConfiguration(tempIniFile.toString(), "SPECIAL");
			assertEquals("/usr/local/bin", readConfig.get("path"));
			assertEquals("Hello World!", readConfig.get("message"));
			assertEquals("äöüß", readConfig.get("special"));
		}
	}

	// ==================== readConfiguration Tests ====================

	@Nested
	@DisplayName("readConfiguration Tests")
	class ReadConfigurationTests
	{
		@Test
		@DisplayName("Sollte Konfiguration aus Datei lesen")
		void readConfiguration_shouldReadConfigFromFile() throws IOException, ConfigFileToolException
		{
			// Arrange
			createTestIniFile(tempIniFile, "[SERVER]\nhost=127.0.0.1\nport=3306\n");

			// Act
			Map<String, String> config = ConfigFileTool.readConfiguration(tempIniFile.toString(), "SERVER");

			// Assert
			assertNotNull(config);
			assertEquals("127.0.0.1", config.get("host"));
			assertEquals("3306", config.get("port"));
		}

		@Test
		@DisplayName("Sollte FileNotFoundException werfen, wenn Datei nicht existiert")
		void readConfiguration_shouldThrowFileNotFoundExceptionForMissingFile()
		{
			// Arrange
			String nonExistentFile = tempDir.resolve("non_existent.ini").toString();

			// Act & Assert
			assertThrows(FileNotFoundException.class, () ->
					ConfigFileTool.readConfiguration(nonExistentFile, "TEST"));
		}

		@Test
		@DisplayName("Sollte ConfigFileToolException werfen, wenn Section nicht existiert")
		void readConfiguration_shouldThrowExceptionForMissingSection() throws IOException
		{
			// Arrange
			createTestIniFile(tempIniFile, "[EXISTING]\nkey=value\n");

			// Act & Assert
			assertThrows(ConfigFileToolException.class, () ->
					ConfigFileTool.readConfiguration(tempIniFile.toString(), "NON_EXISTING"));
		}
	}

	// ==================== readConfigurationFromResources Tests ====================

	@Nested
	@DisplayName("readConfigurationFromResources Tests")
	class ReadConfigurationFromResourcesTests
	{
		@Test
		@DisplayName("Sollte Konfiguration aus Resources lesen")
		void readConfigurationFromResources_shouldReadConfigFromResources() throws IOException, ConfigFileToolException
		{
			// Act
			Map<String, String> configMap = ConfigFileTool.readConfigurationFromResources(RESOURCE_CONFIG_FILE, TEST_SECTION);

			// Assert
			assertNotNull(configMap);
			assertEquals("test", configMap.get("source"));
			assertEquals("Max", configMap.get("firstname"));
			assertEquals("Mustermann", configMap.get("lastname"));
		}

		@Test
		@DisplayName("Sollte IllegalArgumentException werfen, wenn Resource nicht existiert")
		void readConfigurationFromResources_shouldThrowExceptionForMissingResource()
		{
			// Act & Assert
			assertThrows(IllegalArgumentException.class, () ->
					ConfigFileTool.readConfigurationFromResources("non_existent.ini", TEST_SECTION));
		}

		@Test
		@DisplayName("Sollte ConfigFileToolException werfen, wenn Section in Resource nicht existiert")
		void readConfigurationFromResources_shouldThrowExceptionForMissingSection()
		{
			// Act & Assert
			assertThrows(ConfigFileToolException.class, () ->
					ConfigFileTool.readConfigurationFromResources(RESOURCE_CONFIG_FILE, "NON_EXISTING_SECTION"));
		}
	}

	// ==================== readConfiguration (InputStream) Tests ====================

	@Nested
	@DisplayName("readConfiguration (InputStream) Tests")
	class ReadConfigurationFromInputStreamTests
	{
		@Test
		@DisplayName("Sollte Konfiguration aus InputStream lesen")
		void readConfiguration_shouldReadFromInputStream() throws IOException, ConfigFileToolException
		{
			// Arrange
			String iniContent = "[STREAM]\nkey1=value1\nkey2=value2\n";
			InputStream inputStream = new ByteArrayInputStream(iniContent.getBytes(StandardCharsets.UTF_8));

			// Act
			Map<String, String> config = ConfigFileTool.readConfiguration(inputStream, "STREAM");

			// Assert
			assertEquals("value1", config.get("key1"));
			assertEquals("value2", config.get("key2"));
		}

		@Test
		@DisplayName("Sollte ConfigFileToolException werfen, wenn Section nicht im InputStream existiert")
		void readConfiguration_shouldThrowExceptionForMissingSectionInStream()
		{
			// Arrange
			String iniContent = "[EXISTING]\nkey=value\n";
			InputStream inputStream = new ByteArrayInputStream(iniContent.getBytes(StandardCharsets.UTF_8));

			// Act & Assert
			assertThrows(ConfigFileToolException.class, () ->
					ConfigFileTool.readConfiguration(inputStream, "MISSING"));
		}
	}

	// ==================== readIniFileWithAllSections Tests ====================

	@Nested
	@DisplayName("readIniFileWithAllSections Tests")
	class ReadIniFileWithAllSectionsTests
	{
		@Test
		@DisplayName("Sollte alle Sections aus INI-Datei lesen")
		void readIniFileWithAllSections_shouldReadAllSections() throws IOException, ConfigFileToolException
		{
			// Arrange
			String iniContent = "[SECTION1]\nkey1=value1\n\n[SECTION2]\nkey2=value2\n\n[SECTION3]\nkey3=value3\n";
			createTestIniFile(tempIniFile, iniContent);

			// Act
			Map<String, Map<String, String>> allSections = ConfigFileTool.readIniFileWithAllSections(tempIniFile.toString());

			// Assert
			assertEquals(3, allSections.size());
			assertTrue(allSections.containsKey("SECTION1"));
			assertTrue(allSections.containsKey("SECTION2"));
			assertTrue(allSections.containsKey("SECTION3"));
			assertEquals("value1", allSections.get("SECTION1").get("key1"));
			assertEquals("value2", allSections.get("SECTION2").get("key2"));
			assertEquals("value3", allSections.get("SECTION3").get("key3"));
		}

		@Test
		@DisplayName("Sollte ConfigFileToolException werfen, wenn Datei nicht lesbar ist")
		void readIniFileWithAllSections_shouldThrowExceptionForUnreadableFile()
		{
			// Act & Assert
			assertThrows(ConfigFileToolException.class, () ->
					ConfigFileTool.readIniFileWithAllSections(tempDir.resolve("non_existent.ini").toString()));
		}
	}

	// ==================== readIniFileWithAllSectionsFromResources Tests ====================

	@Nested
	@DisplayName("readIniFileWithAllSectionsFromResources Tests")
	class ReadIniFileWithAllSectionsFromResourcesTests
	{
		@Test
		@DisplayName("Sollte alle Sections aus Resource-INI-Datei lesen")
		void readIniFileWithAllSectionsFromResources_shouldReadAllSections() throws ConfigFileToolException
		{
			// Act
			Map<String, Map<String, String>> allSections = ConfigFileTool.readIniFileWithAllSectionsFromResources(RESOURCE_CONFIG_FILE);

			// Assert
			assertNotNull(allSections);
			assertTrue(allSections.containsKey(TEST_SECTION));
			assertEquals("test", allSections.get(TEST_SECTION).get("source"));
		}
	}

	// ==================== getSections Tests ====================

	@Nested
	@DisplayName("getSections Tests")
	class GetSectionsTests
	{
		@Test
		@DisplayName("Sollte alle Section-Namen aus Datei zurückgeben")
		void getSections_shouldReturnAllSectionNames() throws IOException
		{
			// Arrange
			String iniContent = "[ALPHA]\nkey=value\n\n[BETA]\nkey=value\n\n[GAMMA]\nkey=value\n";
			createTestIniFile(tempIniFile, iniContent);

			// Act
			String[] sections = ConfigFileTool.getSections(tempIniFile.toString());

			// Assert
			assertEquals(3, sections.length);
			Set<String> sectionSet = new HashSet<>(Arrays.asList(sections));
			assertTrue(sectionSet.contains("ALPHA"));
			assertTrue(sectionSet.contains("BETA"));
			assertTrue(sectionSet.contains("GAMMA"));
		}

		@Test
		@DisplayName("Sollte leeres Array für leere INI-Datei zurückgeben")
		void getSections_shouldReturnEmptyArrayForEmptyFile() throws IOException
		{
			// Arrange
			createTestIniFile(tempIniFile, "");

			// Act
			String[] sections = ConfigFileTool.getSections(tempIniFile.toString());

			// Assert
			assertEquals(0, sections.length);
		}
	}

	// ==================== getSectionsFromResources Tests ====================

	@Nested
	@DisplayName("getSectionsFromResources Tests")
	class GetSectionsFromResourcesTests
	{
		@Test
		@DisplayName("Sollte Sections aus Resource-Datei zurückgeben")
		void getSectionsFromResources_shouldReturnSectionsFromResource() throws IOException
		{
			// Act
			String[] sections = ConfigFileTool.getSectionsFromResources(RESOURCE_CONFIG_FILE);

			// Assert
			assertNotNull(sections);
			assertTrue(sections.length > 0);
			assertEquals(TEST_SECTION, sections[0]);
		}

		@Test
		@DisplayName("Sollte IllegalArgumentException werfen, wenn Resource nicht existiert")
		void getSectionsFromResources_shouldThrowExceptionForMissingResource()
		{
			// Act & Assert
			assertThrows(IllegalArgumentException.class, () ->
					ConfigFileTool.getSectionsFromResources("non_existent.ini"));
		}
	}

	// ==================== getSectionsAsSet Tests ====================

	@Nested
	@DisplayName("getSectionsAsSet Tests")
	class GetSectionsAsSetTests
	{
		@Test
		@DisplayName("Sollte Sections als Set zurückgeben")
		void getSectionsAsSet_shouldReturnSectionsAsSet() throws IOException
		{
			// Arrange
			String iniContent = "[SET1]\nkey=value\n\n[SET2]\nkey=value\n";
			createTestIniFile(tempIniFile, iniContent);

			// Act
			Set<String> sections = ConfigFileTool.getSectionsAsSet(tempIniFile.toString());

			// Assert
			assertEquals(2, sections.size());
			assertTrue(sections.contains("SET1"));
			assertTrue(sections.contains("SET2"));
		}
	}

	// ==================== getSectionsAsSetFromResource Tests ====================

	@Nested
	@DisplayName("getSectionsAsSetFromResource Tests")
	class GetSectionsAsSetFromResourceTests
	{
		@Test
		@DisplayName("Sollte Sections als Set aus Resource zurückgeben")
		void getSectionsAsSetFromResource_shouldReturnSectionsAsSet() throws IOException
		{
			// Act
			Set<String> sections = ConfigFileTool.getSectionsAsSetFromResource(RESOURCE_CONFIG_FILE);

			// Assert
			assertNotNull(sections);
			assertTrue(sections.contains(TEST_SECTION));
		}

		@Test
		@DisplayName("Sollte IllegalArgumentException werfen, wenn Resource nicht existiert")
		void getSectionsAsSetFromResource_shouldThrowExceptionForMissingResource()
		{
			// Act & Assert
			assertThrows(IllegalArgumentException.class, () ->
					ConfigFileTool.getSectionsAsSetFromResource("non_existent.ini"));
		}
	}

	// ==================== encrypt/decrypt Tests ====================

	@Nested
	@DisplayName("Encryption/Decryption Tests")
	class EncryptionDecryptionTests
	{
		@Test
		@DisplayName("Sollte String verschlüsseln und wieder entschlüsseln können")
		void encryptDecrypt_shouldEncryptAndDecryptSuccessfully() throws GeneralSecurityException, IOException
		{
			// Arrange
			String originalText = "GeheimesPasswort123!";

			// Act
			String encrypted = ConfigFileTool.encrypt(originalText);
			String decrypted = ConfigFileTool.decrypt(encrypted);

			// Assert
			assertNotEquals(originalText, encrypted, "Verschlüsselter Text sollte sich vom Original unterscheiden");
			assertEquals(originalText, decrypted, "Entschlüsselter Text sollte dem Original entsprechen");
		}

		@Test
		@DisplayName("Sollte leeren String verschlüsseln und entschlüsseln können")
		void encryptDecrypt_shouldHandleEmptyString() throws GeneralSecurityException, IOException
		{
			// Arrange
			String emptyText = "";

			// Act
			String encrypted = ConfigFileTool.encrypt(emptyText);
			String decrypted = ConfigFileTool.decrypt(encrypted);

			// Assert
			assertEquals(emptyText, decrypted);
		}

		@Test
		@DisplayName("Sollte Sonderzeichen verschlüsseln und entschlüsseln können")
		void encryptDecrypt_shouldHandleSpecialCharacters() throws GeneralSecurityException, IOException
		{
			// Arrange
			String specialText = "äöü@€ß!#$%^&*()_+-=[]{}|;':\",./<>?";

			// Act
			String encrypted = ConfigFileTool.encrypt(specialText);
			String decrypted = ConfigFileTool.decrypt(encrypted);

			// Assert
			assertEquals(specialText, decrypted);
		}

		@Test
		@DisplayName("Sollte Unicode-Zeichen verschlüsseln und entschlüsseln können")
		void encryptDecrypt_shouldHandleUnicodeCharacters() throws GeneralSecurityException, IOException
		{
			// Arrange
			String unicodeText = "日本語 中文 한국어 العربية";

			// Act
			String encrypted = ConfigFileTool.encrypt(unicodeText);
			String decrypted = ConfigFileTool.decrypt(encrypted);

			// Assert
			assertEquals(unicodeText, decrypted);
		}

		@Test
		@DisplayName("Verschlüsselung sollte konsistente Ergebnisse liefern")
		void encrypt_shouldBeConsistent() throws GeneralSecurityException, UnsupportedEncodingException
		{
			// Arrange
			String text = "TestText";

			// Act
			String encrypted1 = ConfigFileTool.encrypt(text);
			String encrypted2 = ConfigFileTool.encrypt(text);

			// Assert
			assertEquals(encrypted1, encrypted2, "Gleicher Input sollte gleichen verschlüsselten Output erzeugen");
		}

		@Test
		@DisplayName("Sollte langen Text verschlüsseln und entschlüsseln können")
		void encryptDecrypt_shouldHandleLongText() throws GeneralSecurityException, IOException
		{
			// Arrange
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 1000; i++)
			{
				sb.append("LongerTextSegment").append(i);
			}
			String longText = sb.toString();

			// Act
			String encrypted = ConfigFileTool.encrypt(longText);
			String decrypted = ConfigFileTool.decrypt(encrypted);

			// Assert
			assertEquals(longText, decrypted);
		}
	}

	// ==================== Integration Tests ====================

	@Nested
	@DisplayName("Integration Tests")
	class IntegrationTests
	{
		@Test
		@DisplayName("Sollte vollständigen Schreib-Lese-Zyklus durchführen")
		void fullWriteReadCycle_shouldWorkCorrectly() throws IOException, ConfigFileToolException
		{
			// Arrange
			Map<String, String> originalConfig = new HashMap<>();
			originalConfig.put("database", "mysql");
			originalConfig.put("host", "localhost");
			originalConfig.put("port", "3306");
			originalConfig.put("username", "root");
			originalConfig.put("password", "secret");

			// Act - Schreiben
			ConfigFileTool.writeConfiguration(tempIniFile.toString(), "DB_CONFIG", originalConfig);

			// Act - Lesen
			Map<String, String> readConfig = ConfigFileTool.readConfiguration(tempIniFile.toString(), "DB_CONFIG");

			// Assert
			assertEquals(originalConfig.size(), readConfig.size());
			for (Map.Entry<String, String> entry : originalConfig.entrySet())
			{
				assertEquals(entry.getValue(), readConfig.get(entry.getKey()),
							 "Wert für Key '" + entry.getKey() + "' sollte übereinstimmen");
			}
		}

		@Test
		@DisplayName("Sollte mehrere Sections schreiben und lesen können")
		void multipleSections_shouldBeWrittenAndReadCorrectly() throws IOException, ConfigFileToolException
		{
			// Arrange
			String iniContent = "[DATABASE]\nhost=localhost\nport=3306\n\n[SERVER]\nhost=0.0.0.0\nport=8080\n\n[LOGGING]\nlevel=DEBUG\npath=/var/log\n";
			createTestIniFile(tempIniFile, iniContent);

			// Act
			Map<String, Map<String, String>> allSections = ConfigFileTool.readIniFileWithAllSections(tempIniFile.toString());

			// Assert
			assertEquals(3, allSections.size());

			assertEquals("localhost", allSections.get("DATABASE").get("host"));
			assertEquals("3306", allSections.get("DATABASE").get("port"));

			assertEquals("0.0.0.0", allSections.get("SERVER").get("host"));
			assertEquals("8080", allSections.get("SERVER").get("port"));

			assertEquals("DEBUG", allSections.get("LOGGING").get("level"));
			assertEquals("/var/log", allSections.get("LOGGING").get("path"));
		}
	}

	// ==================== Helper Methods ====================

	/**
	 * Hilfsmethode zum Erstellen einer Test-INI-Datei.
	 * Kompatibel mit Java 17.
	 */
	private void createTestIniFile(Path filePath, String content) throws IOException
	{
		try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8))
		{
			writer.write(content);
		}
	}
}