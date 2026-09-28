package org.mosip.nist.nfiq2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of a random forest model (NIST {@code NFIQ2::ModelInfo}), read from its {@code key = value}
 * text file.
 *
 * @param name        model name
 * @param trainer     organisation that trained the model
 * @param description description
 * @param version     model version
 * @param path        model file name
 * @param hash        MD5 of the model YAML text
 */
public record ModelInfo(String name, String trainer, String description, String version, String path,
		String hash) {
	/** Key of the model name. */
	public static final String KEY_NAME = "Name";
	/** Key of the trainer. */
	public static final String KEY_TRAINER = "Trainer";
	/** Key of the description. */
	public static final String KEY_DESCRIPTION = "Description";
	/** Key of the version. */
	public static final String KEY_VERSION = "Version";
	/** Key of the model file name. */
	public static final String KEY_PATH = "Path";
	/** Key of the MD5 hash. */
	public static final String KEY_HASH = "Hash";

	/**
	 * Reads a model information file. {@code Path} and {@code Hash} are required.
	 *
	 * @param in text (the stream is not closed)
	 * @return model information
	 * @throws IOException    when reading fails
	 * @throws Nfiq2Exception {@link ErrorCode#InvalidConfiguration} when a required key is missing
	 */
	public static ModelInfo read(InputStream in) throws IOException {
		Map<String, String> values = new HashMap<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		String line;
		while ((line = reader.readLine()) != null) {
			int eq = line.indexOf('=');
			if (eq > 0 && !line.trim().startsWith("#")) {
				values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
			}
		}
		for (String required : new String[] { KEY_PATH, KEY_HASH }) {
			if (values.getOrDefault(required, "").isEmpty()) {
				throw new Nfiq2Exception(ErrorCode.InvalidConfiguration,
						"Required key " + required + " not found in model information");
			}
		}
		return new ModelInfo(values.get(KEY_NAME), values.get(KEY_TRAINER), values.get(KEY_DESCRIPTION),
				values.get(KEY_VERSION), values.get(KEY_PATH), values.get(KEY_HASH));
	}
}
