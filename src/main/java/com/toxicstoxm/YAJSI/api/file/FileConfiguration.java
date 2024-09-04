package com.toxicstoxm.YAJSI.api.file;


import com.toxicstoxm.YAJSI.api.yaml.Configuration;
import com.toxicstoxm.YAJSI.api.yaml.InvalidConfigurationException;
import com.toxicstoxm.YAJSI.api.yaml.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * This is a base class for all File-based implementations of {@link
 * Configuration}
 */
public abstract class FileConfiguration extends MemoryConfiguration {

    /**
     * Creates an empty {@link FileConfiguration} with no default values.
     */
    public FileConfiguration() {
        super();
    }

    /**
     * Creates an empty {@link FileConfiguration} using the specified {@link
     * Configuration} as a source for all default values.
     *
     * @param defaults Default value provider
     */
    public FileConfiguration(@Nullable Configuration defaults) {
        super(defaults);
    }

    /**
     * Saves this {@link FileConfiguration} to the specified location.
     * <p>
     * If the file does not exist, it will be created. If it already exists, it
     * will be overwritten. If it cannot be overwritten or created, an
     * exception will be thrown.
     * <p>
     * This method will save using the system's default encoding, or possibly
     * using UTF8.
     *
     * @param file File to save to.
     * @throws IOException Thrown when the given file cannot be written to for
     *     any reason.
     * @throws IllegalArgumentException Thrown when a file is null.
     */
    public void save(@NotNull File file) throws IOException {
        file.getParentFile().mkdirs();
        //Files.createParentDirs(file);

        String data = saveToString();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(data);
        }
    }

    /**
     * Saves this {@link FileConfiguration} to the specified location.
     * <p>
     * If the file does not exist, it will be created. If it already exists, it
     * will be overwritten. If it cannot be overwritten or created, an
     * exception will be thrown.
     * <p>
     * This method will save using the system's default encoding, or possibly
     * using UTF8.
     *
     * @param file File to save to.
     * @throws IOException Thrown when the given file cannot be written to for
     *     any reason.
     * @throws IllegalArgumentException Thrown when the file is null.
     */
    public void save(@NotNull String file) throws IOException {
        save(new File(file));
    }

    /**
     * Saves this {@link FileConfiguration} to a string, and returns it.
     *
     * @return String containing this configuration.
     */
    @NotNull
    public abstract String saveToString();

    /**
     * Loads this {@link FileConfiguration} from the specified location.
     * <p>
     * All the values contained within this configuration will be removed,
     * leaving only settings and defaults, and the new values will be loaded
     * from the given file.
     * <p>
     * If the file cannot be loaded for any reason, an exception will be
     * thrown.
     *
     * @param file File to load from.
     * @throws FileNotFoundException Thrown when the given file cannot be
     *     opened.
     * @throws IOException Thrown when the given file cannot be read.
     * @throws InvalidConfigurationException Thrown when the given file is not
     *     a valid Configuration.
     * @throws IllegalArgumentException Thrown when the file is null.
     */
    public void load(@NotNull File file) throws FileNotFoundException, IOException, InvalidConfigurationException {
        final FileInputStream stream = new FileInputStream(file);

        load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    /**
     * Loads this {@link FileConfiguration} from the specified reader.
     * <p>
     * All the values contained within this configuration will be removed,
     * leaving only settings and defaults, and the new values will be loaded
     * from the given stream.
     *
     * @param reader the reader to load from
     * @throws IOException thrown when underlying reader throws an IOException
     * @throws InvalidConfigurationException thrown when the reader does not
     *      represent a valid Configuration
     * @throws IllegalArgumentException thrown when reader is null
     */
    public void load(@NotNull Reader reader) throws IOException, InvalidConfigurationException {
        BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);

        StringBuilder builder = new StringBuilder();

        try {
            String line;

            while ((line = input.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
        } finally {
            input.close();
        }

        loadFromString(builder.toString());
    }

    /**
     * Loads this {@link FileConfiguration} from the specified location.
     * <p>
     * All the values contained within this configuration will be removed,
     * leaving only settings and defaults, and the new values will be loaded
     * from the given file.
     * <p>
     * If the file cannot be loaded for any reason, an exception will be
     * thrown.
     *
     * @param file File to load from.
     * @throws FileNotFoundException Thrown when the given file cannot be
     *     opened.
     * @throws IOException Thrown when the given file cannot be read.
     * @throws InvalidConfigurationException Thrown when the given file is not
     *     a valid Configuration.
     * @throws IllegalArgumentException Thrown when the file is null.
     */
    public void load(@NotNull String file) throws FileNotFoundException, IOException, InvalidConfigurationException {
        load(new File(file));
    }

    /**
     * Loads this {@link FileConfiguration} from the specified string, as
     * opposed to from the file.
     * <p>
     * All the values contained within this configuration will be removed,
     * leaving only settings and defaults, and the new values will be loaded
     * from the given string.
     * <p>
     * If the string is invalid in any way, an exception will be thrown.
     *
     * @param contents Contents of a Configuration to load.
     * @throws InvalidConfigurationException Thrown if the specified string is
     *     invalid.
     * @throws IllegalArgumentException Thrown if contents is null.
     */
    public abstract void loadFromString(@NotNull String contents) throws InvalidConfigurationException;

    /**
     * @return empty string
     *
     * @deprecated This method only exists for backwards compatibility. It will
     * do nothing and should not be used! Please use
     * {@link FileConfigurationOptions#getHeader()} instead.
     */
    @NotNull
    @Deprecated
    protected String buildHeader() {
        return "";
    }

    @NotNull
    @Override
    public FileConfigurationOptions options() {
        if (options == null) {
            options = new FileConfigurationOptions(this);
        }

        return (FileConfigurationOptions) options;
    }
}
