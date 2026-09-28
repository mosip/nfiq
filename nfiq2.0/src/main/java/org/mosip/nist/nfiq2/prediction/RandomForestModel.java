package org.mosip.nist.nfiq2.prediction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.Nfiq2Exception;

/**
 * OpenCV {@code cv::ml::RTrees} classifier read from its YAML (format 3) serialisation, evaluated with
 * {@code RAW_OUTPUT} (NIST {@code prediction/RandomForestML.cpp}).
 * <p>
 * Only ordered splits are supported, which is what the NIST models contain. The MD5 digest of the YAML text
 * is computed while reading.
 */
public final class RandomForestModel {
	/** {@code FLT_MAX}, OpenCV's marker for a missing value. */
	private static final float MISSED_VAL = Float.MAX_VALUE;
	/** {@code key:value} tokens inside the tree list. */
	private static final Pattern TOKEN = Pattern.compile("\\b(depth|value|var|le|gt|in|not_in):\\s*([^,}\\s]+)");
	/** {@code key: value} lines of the header. */
	private static final Pattern HEADER = Pattern.compile("^\\s*(\\w+):\\s*(.*)$");

	/** Number of input variables. */
	private int varCount = -1;
	/** Declared number of trees. */
	private int declaredTrees = -1;
	/** Classifier flag. */
	private boolean classifier;
	/** Number of class labels. */
	private int classCount;
	/** Substitutes for missing values. */
	private float[] missingSubst;

	/** Root node of each tree. */
	private int[] roots = new int[16];
	/** Number of trees read. */
	private int treeCount;
	/** Left child per node, or -1. */
	private int[] left = new int[1024];
	/** Right child per node, or -1. */
	private int[] right = new int[1024];
	/** Parent per node, or -1. */
	private int[] parent = new int[1024];
	/** Split variable per node, or -1 for leaves. */
	private int[] splitVar = new int[1024];
	/** Split threshold per node. */
	private float[] threshold = new float[1024];
	/** Node value. */
	private double[] value = new double[1024];
	/** Number of nodes read. */
	private int nodeCount;
	/** Lower-case hex MD5 of the YAML text. */
	private final String hash;

	/**
	 * Reads a model.
	 *
	 * @param yaml YAML text (the stream is not closed)
	 * @throws IOException    when reading fails
	 * @throws Nfiq2Exception {@link ErrorCode#InvalidConfiguration} when the model is malformed
	 */
	public RandomForestModel(InputStream yaml) throws IOException {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new Nfiq2Exception(ErrorCode.UnknownError, "MD5 is not available", e);
		}
		DigestInputStream in = new DigestInputStream(yaml, md5);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
		parse(reader);
		while (reader.read() >= 0) {
			// drain so that the digest covers the whole text
		}
		hash = HexFormat.of().formatHex(md5.digest());
		validate();
	}

	/**
	 * Returns the MD5 digest of the YAML text.
	 *
	 * @return lower-case hex digest
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * Returns the number of trees.
	 *
	 * @return tree count
	 */
	public int getTreeCount() {
		return treeCount;
	}

	/**
	 * Returns the number of input variables.
	 *
	 * @return variable count
	 */
	public int getVarCount() {
		return varCount;
	}

	/**
	 * {@code RTrees::predict(sample, noArray(), RAW_OUTPUT)} for a two-class classifier: sum of the leaf
	 * values over all trees.
	 *
	 * @param sample input variables, already converted to {@code float}
	 * @return raw prediction in {@code 0..treeCount}
	 * @throws Nfiq2Exception {@link ErrorCode#MachineLearningError} when the sample size is wrong
	 */
	public float predictRaw(float[] sample) {
		if (sample.length != varCount) {
			throw new Nfiq2Exception(ErrorCode.MachineLearningError,
					"Sample has " + sample.length + " values, the model expects " + varCount);
		}
		double sum = 0;
		for (int t = 0; t < treeCount; t++) {
			int nidx = roots[t];
			int prev = nidx;
			while (nidx >= 0) {
				prev = nidx;
				int vi = splitVar[nidx];
				if (vi < 0) {
					break;
				}
				float val = sample[vi];
				if (val == MISSED_VAL) {
					val = missingSubst[vi];
				}
				nidx = val <= threshold[nidx] ? left[nidx] : right[nidx];
			}
			sum += value[prev];
		}
		return (float) sum;
	}

	/**
	 * Parses the header and the tree list.
	 *
	 * @param reader YAML lines
	 * @throws IOException when reading fails
	 */
	private void parse(BufferedReader reader) throws IOException {
		String line;
		boolean inTrees = false;
		int pidx = -1;
		int current = -1;
		boolean currentHasSplit = false;
		while ((line = reader.readLine()) != null) {
			String trimmed = line.trim();
			if (!inTrees) {
				Matcher h = HEADER.matcher(line);
				if (!h.matches()) {
					continue;
				}
				String key = h.group(1);
				String rest = h.group(2).trim();
				switch (key) {
				case "is_classifier" -> classifier = Integer.parseInt(rest) != 0;
				case "var_count" -> varCount = Integer.parseInt(rest);
				case "ntrees" -> declaredTrees = Integer.parseInt(rest);
				case "class_labels" -> classCount = readList(reader, rest).length;
				case "missing_subst" -> missingSubst = toFloats(readList(reader, rest));
				case "trees" -> inTrees = true;
				default -> {
					if (rest.startsWith("[")) {
						readList(reader, rest);
					}
				}
				}
				continue;
			}
			if (trimmed.equals("nodes:")) {
				pidx = -1;
				current = -1;
				continue;
			}
			Matcher m = TOKEN.matcher(line);
			while (m.find()) {
				String key = m.group(1);
				String val = m.group(2);
				switch (key) {
				case "depth" -> {
					if (current >= 0) {
						pidx = link(current, pidx, currentHasSplit);
					}
					current = newNode();
					currentHasSplit = false;
					if (pidx < 0) {
						addRoot(current);
					} else {
						parent[current] = pidx;
						if (left[pidx] < 0) {
							left[pidx] = current;
						} else {
							right[pidx] = current;
						}
					}
				}
				case "value" -> value[requireNode(current)] = Double.parseDouble(val);
				case "var" -> {
					if (!currentHasSplit) {
						splitVar[requireNode(current)] = Integer.parseInt(val);
					}
				}
				case "le", "gt" -> {
					if (!currentHasSplit) {
						threshold[requireNode(current)] = (float) Double.parseDouble(val);
						currentHasSplit = true;
					}
				}
				default -> throw new Nfiq2Exception(ErrorCode.InvalidConfiguration,
						"Categorical splits are not supported");
				}
			}
		}
	}

	/**
	 * Finishes a node: {@code readTree}'s parent tracking.
	 *
	 * @param nidx     node
	 * @param pidx     current parent
	 * @param hasSplit whether the node is internal
	 * @return next parent
	 */
	private int link(int nidx, int pidx, boolean hasSplit) {
		if (!hasSplit) {
			int p = pidx;
			while (p >= 0 && right[p] >= 0) {
				p = parent[p];
			}
			return p;
		}
		return nidx;
	}

	/**
	 * Allocates a node.
	 *
	 * @return node index
	 */
	private int newNode() {
		if (nodeCount == left.length) {
			int n = left.length * 2;
			left = Arrays.copyOf(left, n);
			right = Arrays.copyOf(right, n);
			parent = Arrays.copyOf(parent, n);
			splitVar = Arrays.copyOf(splitVar, n);
			threshold = Arrays.copyOf(threshold, n);
			value = Arrays.copyOf(value, n);
		}
		int i = nodeCount++;
		left[i] = -1;
		right[i] = -1;
		parent[i] = -1;
		splitVar[i] = -1;
		return i;
	}

	/**
	 * Registers a tree root.
	 *
	 * @param nidx root node
	 */
	private void addRoot(int nidx) {
		if (treeCount == roots.length) {
			roots = Arrays.copyOf(roots, roots.length * 2);
		}
		roots[treeCount++] = nidx;
	}

	/**
	 * Guards against values outside a node.
	 *
	 * @param nidx node index
	 * @return {@code nidx}
	 */
	private static int requireNode(int nidx) {
		if (nidx < 0) {
			throw new Nfiq2Exception(ErrorCode.InvalidConfiguration, "Malformed random forest: value outside node");
		}
		return nidx;
	}

	/**
	 * Reads a YAML flow sequence that may span several lines.
	 *
	 * @param reader lines
	 * @param first  text after the key
	 * @return items
	 * @throws IOException when reading fails
	 */
	private static String[] readList(BufferedReader reader, String first) throws IOException {
		StringBuilder sb = new StringBuilder(first);
		String line;
		while (sb.indexOf("]") < 0 && (line = reader.readLine()) != null) {
			sb.append(' ').append(line.trim());
		}
		int open = sb.indexOf("[");
		int close = sb.indexOf("]");
		if (open < 0 || close < open) {
			throw new Nfiq2Exception(ErrorCode.InvalidConfiguration, "Malformed random forest list: " + first);
		}
		String body = sb.substring(open + 1, close).trim();
		return body.isEmpty() ? new String[0] : body.split("\\s*,\\s*");
	}

	/**
	 * Parses numbers as {@code float}.
	 *
	 * @param items numbers
	 * @return values
	 */
	private static float[] toFloats(String[] items) {
		float[] out = new float[items.length];
		for (int i = 0; i < items.length; i++) {
			out[i] = (float) Double.parseDouble(items[i]);
		}
		return out;
	}

	/**
	 * Checks the model after parsing.
	 *
	 * @throws Nfiq2Exception {@link ErrorCode#InvalidConfiguration} when the model is unusable
	 */
	private void validate() {
		if (!classifier || classCount != 2) {
			throw new Nfiq2Exception(ErrorCode.InvalidConfiguration,
					"The trained network could not be loaded for prediction!");
		}
		if (varCount <= 0 || treeCount == 0 || (declaredTrees >= 0 && declaredTrees != treeCount)) {
			throw new Nfiq2Exception(ErrorCode.InvalidConfiguration, "Malformed random forest: " + treeCount
					+ " trees read, " + declaredTrees + " declared, " + varCount + " variables");
		}
		if (missingSubst == null) {
			missingSubst = new float[varCount];
		}
		if (missingSubst.length < varCount) {
			throw new Nfiq2Exception(ErrorCode.InvalidConfiguration, "Malformed random forest: missing_subst");
		}
		for (int i = 0; i < nodeCount; i++) {
			if (splitVar[i] >= varCount) {
				throw new Nfiq2Exception(ErrorCode.InvalidConfiguration,
						"Malformed random forest: split variable " + splitVar[i]);
			}
		}
	}
}
