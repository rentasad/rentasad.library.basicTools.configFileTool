# ConfigFileTool

A lightweight Java library for reading and writing INI configuration files with optional encryption support.

---

## Features

* Read and write INI files with section-based configuration
* Load configurations from classpath resources
* Support for multiple sections in a single INI file
* Built-in encryption/decryption for sensitive configuration values (`PBEWithMD5AndDES`)
* Java 17+ compatible
* Built on top of **Apache Commons Configuration2** (actively maintained)

---

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>rentasad.library</groupId>
    <artifactId>rentasad.library.basicTools.configFileTool</artifactId>
    <version>3.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'rentasad.library:rentasad.library.basicTools.configFileTool:3.0.0'
```

---

## Quick Start

### Reading configuration from a file

```java
import rentasad.library.configFileTool.ConfigFileTool;
import rentasad.library.configFileTool.ConfigFileToolException;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Map;

public class Example {
    public static void main(String[] args) {
        try {
            Map<String, String> config = ConfigFileTool.readConfiguration("config/database.ini", "DATABASE");

            String host = config.get("host");
            String port = config.get("port");
            String username = config.get("username");

            System.out.printf("Connecting to %s:%s as %s%n", host, port, username);
        } catch (FileNotFoundException e) {
            System.err.println("Configuration file not found: " + e.getMessage());
        } catch (ConfigFileToolException e) {
            System.err.println("Section not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading configuration: " + e.getMessage());
        }
    }
}
```

### Reading configuration from resources (classpath)

```java
import java.util.Map;

Map<String, String> config = ConfigFileTool.readConfigurationFromResources(
        "config/app.ini",
        "SERVER"
);
```

### Writing configuration to a file

```java
import java.util.HashMap;
import java.util.Map;

Map<String, String> configMap = new HashMap<>();
configMap.put("host", "localhost");
configMap.put("port", "3306");
configMap.put("username", "admin");

ConfigFileTool.writeConfiguration("config/database.ini", "DATABASE", configMap);
```

### Reading all sections from an INI file

```java
Map<String, Map<String, String>> allSections =
        ConfigFileTool.readIniFileWithAllSections("config/app.ini");

for (String sectionName : allSections.keySet()) {
    System.out.println("Section: " + sectionName);
    Map<String, String> sectionConfig = allSections.get(sectionName);
    sectionConfig.forEach((key, value) ->
            System.out.println("  " + key + " = " + value)
    );
}
```

### Getting section names

```java
String[] sections = ConfigFileTool.getSections("config/app.ini");
Set<String> sectionSet = ConfigFileTool.getSectionsAsSet("config/app.ini");

String[] resourceSections = ConfigFileTool.getSectionsFromResources("config/app.ini");
Set<String> resourceSectionSet = ConfigFileTool.getSectionsAsSetFromResource("config/app.ini");
```

### Encrypting and decrypting values

```java
String encryptedPassword = ConfigFileTool.encrypt("mySecretPassword");
System.out.println("Encrypted: " + encryptedPassword);

String decryptedPassword = ConfigFileTool.decrypt(encryptedPassword);
System.out.println("Decrypted: " + decryptedPassword);
```

---

## INI File Format

The library supports the standard INI file format with sections and key-value pairs:

```ini
[DATABASE]
host=localhost
port=3306
username=admin
password=secret

[SERVER]
host=0.0.0.0
port=8080
maxConnections=100

[LOGGING]
level=DEBUG
path=/var/log/app
```

---

## API Overview

### Read operations

| Method                                                            | Description                                            |
| ----------------------------------------------------------------- | ------------------------------------------------------ |
| `readConfiguration(String fileName, String section)`              | Reads configuration from a file for a specific section |
| `readConfigurationFromResources(String fileName, String section)` | Reads configuration from classpath resources           |
| `readConfiguration(InputStream inputStream, String section)`      | Reads configuration from an `InputStream`              |
| `readIniFileWithAllSections(String fileName)`                     | Reads all sections from a file                         |
| `readIniFileWithAllSectionsFromResources(String fileName)`        | Reads all sections from classpath resources            |

### Write operations

| Method                                                                               | Description                    |
| ------------------------------------------------------------------------------------ | ------------------------------ |
| `writeConfiguration(String fileName, String section, Map<String, String> configMap)` | Writes configuration to a file |

### Section retrieval

| Method                                          | Description                                           |
| ----------------------------------------------- | ----------------------------------------------------- |
| `getSections(String fileName)`                  | Returns section names as `String[]`                   |
| `getSectionsAsSet(String fileName)`             | Returns section names as `Set<String>`                |
| `getSectionsFromResources(String fileName)`     | Returns section names from resources as `String[]`    |
| `getSectionsAsSetFromResource(String fileName)` | Returns section names from resources as `Set<String>` |

### Encryption

| Method                     | Description                                |
| -------------------------- | ------------------------------------------ |
| `encrypt(String property)` | Encrypts a string using `PBEWithMD5AndDES` |
| `decrypt(String property)` | Decrypts a previously encrypted string     |

---

## Exception Handling

The library may throw the following exceptions:

* **`ConfigFileToolException`** – Thrown when a section is not found or other configuration-related errors occur.
* **`FileNotFoundException`** – Thrown when the specified file does not exist.
* **`IOException`** – Thrown for general I/O errors.
* **`IllegalArgumentException`** – Thrown when a resource file is not found in the classpath.

Example:

```java
try {
    Map<String, String> config = ConfigFileTool.readConfiguration("config.ini", "DATABASE");
} catch (FileNotFoundException e) {
    System.err.println("Configuration file not found: " + e.getMessage());
} catch (ConfigFileToolException e) {
    System.err.println("Section not found: " + e.getMessage());
} catch (IOException e) {
    System.err.println("Error reading configuration: " + e.getMessage());
}
```

---

## Requirements

* Java 17 or higher
* Maven 3.6+ (for building)

---

## Building from Source

```bash
git clone https://github.com/rentasad/rentasad.library.basicTools.configFileTool.git
cd rentasad.library.basicTools.configFileTool
mvn clean install
mvn test
```

---

## License

This project is licensed under the terms specified in the repository's `LICENSE` file.
