/*
     * copyright: Anthony Bagnall
 * A filter for using the shapelet transform with hierarchical
 * clustering of shapelets.
 * 
 * Recommended usage: Build the shapelet transform outside of this class and pass in. 
 * 
 * ShapeletTransform shape=new  ShapeletTransform();
 * //Build and use shape here
 *
 * int nosClusters=10;
 * ClusteredShapeletTransform cShape=new ClusteredShapeletTransform(shape,nosClusters);
 * 
 * it will work like this with any of the numerous constructors
  * ClusteredShapeletTransform cShape=new ClusteredShapeletTransform();
  * Instances c=cShape.process(data)
  * 
  * 
 
 
 */
package weka.filters.timeseries.shapelet_transforms;

import java.util.ArrayList;
import java.util.Arrays;

import com.lw.shapelet.QualityBound;
import com.lw.shapelet.QualityMeasures;
import com.lw.shapelet.Shapelet;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.SimpleBatchFilter;

/**
 *
 * @author Jon Hills - j.hills@uea.ac.uk
 */
public class ClusteredShapeletTransform extends SimpleBatchFilter {

	ShapeletTransform st;
	protected double[][] distanceMap;
	protected ArrayList<int[]> clusterPairs;
	public ArrayList<Shapelet> clusteredShapelets;
	public ArrayList<Shapelet> allShapelets;
	protected int noClust;
	protected boolean shapeletsTrained;
	public static int DEFAULT_NUMCLUSTERS = 1;
	protected ArrayList<Shapelet> shapelets;

	/* 
	 * 
	 */
	public ClusteredShapeletTransform(ShapeletTransform shapes, int n) {
		st = shapes;
		this.clusteredShapelets = new ArrayList<>();
		noClust = n;
		this.shapeletsTrained = false;
		this.shapelets = new ArrayList<Shapelet>();
	}

	/**
	 * Fully specified constructor.
	 * 
	 * @param k
	 *            The number of shapelets to store.
	 * @param minShapeletLength
	 *            The minimum shapelet langth.
	 * @param maxShapeletLength
	 *            The maximum shapelet length.
	 * @param qualityChoice
	 *            The quality measure to use for assessing candidates.
	 * @param cluster
	 *            Whether to cluster the shapelets for the transform.
	 * @param noClust
	 *            The number of clusters.
	 */
	public ClusteredShapeletTransform(int k, int minShapeletLength, int maxShapeletLength,
			QualityMeasures.ShapeletQualityChoice qualityChoice, int noClust) {
		st = new ShapeletTransform(k, minShapeletLength, maxShapeletLength, qualityChoice);
		this.noClust = noClust;
		this.clusteredShapelets = new ArrayList<>();
	}

	/**
	 * Partially specified constructor. Defaults to clustering. If clustering is
	 * used, defaults to one cluster, i.e., the best shapelet only.
	 * 
	 * @param k
	 *            The number of shapelets to store.
	 * @param minShapeletLength
	 *            The minimum shapelet langth.
	 * @param maxShapeletLength
	 *            The maximum shapelet length.
	 * @param qualityChoice
	 *            The quality measure to use for assessing candidates.
	 */
	public ClusteredShapeletTransform(int k, int minShapeletLength, int maxShapeletLength,
			QualityMeasures.ShapeletQualityChoice qualityChoice) {
		st = new ShapeletTransform(k, minShapeletLength, maxShapeletLength, qualityChoice);
		this.noClust = DEFAULT_NUMCLUSTERS;
		this.clusteredShapelets = new ArrayList<>();
	}

	/**
	 * Partially specified constructor. Defaults to Information Gain quality
	 * measure. Defaults to no clustering. If clustering is used, defaults to
	 * one cluster, i.e., the best shapelet only.
	 * 
	 * @param k
	 *            The number of shapelets to store.
	 * @param minShapeletLength
	 *            The minimum shapelet langth.
	 * @param maxShapeletLength
	 *            The maximum shapelet length.
	 */
	public ClusteredShapeletTransform(int k, int minShapeletLength, int maxShapeletLength) {
		st = new ShapeletTransform(k, minShapeletLength, maxShapeletLength);
		this.noClust = DEFAULT_NUMCLUSTERS;
		this.clusteredShapelets = new ArrayList<>();
	}

	/**
	 * Partially specified constructor. Defaults to Information Gain quality
	 * measure.
	 * 
	 * @param k
	 *            The number of shapelets to store.
	 * @param minShapeletLength
	 *            The minimum shapelet langth.
	 * @param maxShapeletLength
	 *            The maximum shapelet length.
	 * @param cluster
	 *            Whether to cluster the shapelets for the transform.
	 * @param noClust
	 *            The number of clusters.
	 */
	public ClusteredShapeletTransform(int k, int minShapeletLength, int maxShapeletLength, int noClust) {
		st = new ShapeletTransform(k, minShapeletLength, maxShapeletLength);
		this.noClust = noClust;
		this.clusteredShapelets = new ArrayList<>();
	}

	/**
	 * Partially specified constructor. Defaults to Information Gain quality
	 * measure. Minimum and maximum shapelet lengths must be set before use.
	 * Defaults to no clustering. Defaults to one cluster.
	 * 
	 * @param k
	 *            The number of shapelets to store.
	 */
	public ClusteredShapeletTransform(int k) {
		st = new ShapeletTransform(k);
		this.noClust = DEFAULT_NUMCLUSTERS;
		this.clusteredShapelets = new ArrayList<>();
	}

	/**
	 * Partially specified constructor. Defaults to Information Gain quality
	 * measure. Minimum and maximum shapelet lengths must be set before use.
	 * 
	 * @param k
	 *            The number of shapelets to store.
	 * @param cluster
	 *            Whether or not to use clustering.
	 * @param noClust
	 *            Then number of clusters.
	 */
	public ClusteredShapeletTransform(int k, boolean cluster, int noClust) {
		st = new ShapeletTransform(k);
		this.noClust = noClust;
		this.clusteredShapelets = new ArrayList<>();
	}

	/**
	 * Empty constructor. Defaults to Information Gain quality measure, no
	 * clustering, one cluster if clustering turned on. Shapelet lengths must be
	 * set. K must be set.
	 */
	public ClusteredShapeletTransform() {
		st = new ShapeletTransform();
		this.noClust = DEFAULT_NUMCLUSTERS;
		this.clusteredShapelets = new ArrayList<>();
	}

	/**
	 * Transform datasets. If cluster=true, shapelets will be clustered into
	 * noClust clusters prior to transformation.
	 * 
	 * @param data
	 *            - the input data to be transformed (and to find the shapelets
	 *            if this is the first run)
	 * @return the transformed Instances in terms of the distance from each
	 *         shapelet
	 * @throws Exception
	 *             - if the number of shapelets or the length parameters
	 *             specified are incorrect
	 */
	@Override
	public Instances process(Instances data) throws Exception {
		int size = st.getNumberOfShapelets();
		if (size < 1)
			throw new Exception(
					"Number of shapelets initialised incorrectly - please select value of k (Usage: setNumberOfShapelets");
		if (size < noClust)
			throw new Exception("Trying to produce more clusters than there are shapelets!");
		// We only want the shapelets from st, so could optimize this to not
		// work out the transform too. However, cleaner this way

		if (!st.foundShapelets())
			st.process(data);
		allShapelets = st.shapelets;
		System.out.println("allshapelets.size = " + allShapelets.size());
		/*
		 * for(int i = 0;i<allShapelets.get(0).content.length;i++){
		 * System.out.print(allShapelets.get(0).content[i]+";"); }
		 */
		clusterShapelets();
		Instances output = calDistance(data, clusteredShapelets);
		return output;
	}

	protected Instances calDistance(Instances data, ArrayList<Shapelet> shapeletT) throws Exception {
		Instances output = determineOutputFormat(data);
		for (int i = 0; i < data.numInstances(); i++) { // for each data
			Instance toAdd = new Instance(shapeletT.size() + 1);
			int shapeletNum = 0;
			for (Shapelet s : shapeletT) {
				double dist = ShapeletTransform.subsequenceDistance(s.content, data.instance(i));
				toAdd.setValue(shapeletNum++, dist);
			}
			toAdd.setValue(shapeletT.size(), data.instance(i).classValue());
			output.add(toAdd);
		}
		return output;
	}

	/**
	 *
	 * @param inputFormat
	 *            - the format of the input data
	 * @return a new Instances object in the desired output format
	 * @throws Exception
	 *             - if all required attributes of the filter are not
	 *             initialised correctly
	 */
	@Override
	protected Instances determineOutputFormat(Instances inputFormat) throws Exception {

		/*
		 * int s=st.getNumberOfShapelets(); if(s < 1 || s<noClust){ throw new
		 * Exception("ShapeletFilter not initialised correctly - please specify a value of k that is greater than or equal to 1. You entered s="
		 * +s+" num clusters ="+noClust); }
		 */
		FastVector atts = new FastVector();
		String name;
		for (int i = 0; i < noClust; i++) {
			name = "CShapelet_" + i;
			atts.addElement(new Attribute(name));
		}
		Attribute target = inputFormat.attribute(inputFormat.classIndex());

		FastVector vals = new FastVector(target.numValues());
		for (int i = 0; i < target.numValues(); i++) {
			vals.addElement(target.value(i));
		}
		atts.addElement(new Attribute(inputFormat.attribute(inputFormat.classIndex()).name(), vals));
		Instances result = new Instances("CShapelets" + inputFormat.relationName(), atts, inputFormat.numInstances());
		result.setClassIndex(result.numAttributes() - 1);
		return result;
	}

	/**
	 * Creates a set of clustered shapelets with a noClust clusters.
	 */
	public void clusterShapelets() {
		// System.out.println("Clustering shapelets: "+this.noClust);

		double[][] shapeletSet = new double[allShapelets.size()][];

		for (int i = 0; i < shapeletSet.length; i++) {
			shapeletSet[i] = allShapelets.get(i).content;
		}

		distanceMap = getDistanceMap(shapeletSet);
		clusterPairs = new ArrayList();
		this.clusteredShapelets.clear();

		// Adds an int[] of each index to clusterPairs
		for (int i = 0; i < distanceMap.length; i++) {
			int[] tmp = { i };
			clusterPairs.add(tmp);
		}

		// Returns pair of indexes to clusterPairs/adjusted distanceMap
		// Is the index of the ArrayList ever a factor? It should be done with
		// just the stored indexes.
		int[] bestPair = findClosestPair(distanceMap);
		double[][] map = new double[2][];

		while (clusterPairs.size() > noClust) {
			adjustClusterPairs(bestPair);
			map = adjustDistanceMap();
			bestPair = findClosestPair(map);
		}

		// Select the best shapelet in each cluster
		// Make sure that the index stored in clusterPairs is the index of
		// the shapelet stored in the shapelet ArrayList.
		for (int i = 0; i < clusterPairs.size(); i++) {
			if (clusterPairs.get(i).length == 1)
				clusteredShapelets.add(allShapelets.get(clusterPairs.get(i)[0]));
			else {
				double best = Double.MIN_VALUE;
				int position = 0;

				for (int j = 0; j < clusterPairs.get(i).length; j++) {
					// Infogain will need to be changed to quality measure
					if (allShapelets.get(clusterPairs.get(i)[j]).qualityValue > best) {
						best = allShapelets.get(clusterPairs.get(i)[j]).qualityValue;
						position = j;
					}
				}

				clusteredShapelets.add(allShapelets.get(clusterPairs.get(i)[position]));
				// System.out.println("Added shapelet at position"+position);
			}

		}

	}

	/**
	 * Finds the pair on a distance map with the least distance between them.
	 * 
	 * @param map
	 *            The current distance map
	 * @return The indexes of the best-matching pair.
	 */
	private int[] findClosestPair(double[][] map) {
		int[] pair = new int[2];
		double best = Double.MAX_VALUE;

		for (int i = 0; i < map.length; i++) {
			for (int j = i + 1; j < map[i].length; j++) {
				if (map[i][j] < best) {
					best = map[i][j];
					pair[0] = i;
					pair[1] = j;
				}
			}
		}

		return pair;
	}

	/**
	 * Creates complete distance map with identities and redundant information.
	 * 
	 * @param shapeletSet
	 *            An array of shapelet content double arrays.
	 * @return The distance map for the shapelet set.
	 */
	private double[][] getDistanceMap(double[][] shapeletSet) {

		double[][] map = new double[shapeletSet.length][];

		// Initialise double[]
		for (int i = 0; i < shapeletSet.length; i++) {
			double[] tmp = new double[shapeletSet.length];
			map[i] = tmp;
		}

		for (int i = 0; i < shapeletSet.length; i++) {
			map[i][i] = 0;

			for (int j = i + 1; j < shapeletSet.length; j++) {
				map[i][j] = findMinDistance(shapeletSet[i], shapeletSet[j]);
				map[j][i] = map[i][j];
			}

		}

		return map;
	}

	/**
	 * Returns the shapelet distance between two shapelets, that is, the
	 * shortest distance between the shorter shapelet and the best-matching
	 * subsequence of the longer shapelet.
	 * 
	 * @param first
	 *            One shapelet content array.
	 * @param second
	 *            The other shapelet content array.
	 * @return The shapelet distance between the shapelets.
	 */
	private double findMinDistance(double[] first, double[] second) {

		double distance = 0;
		double bestDist = Double.MAX_VALUE;

		if (first.length == second.length) {
			bestDist = getDistance(first, second);
		} else {
			if (first.length > second.length) {
				for (int i = 0; i < (first.length - second.length) + 1; i++) {
					double[] temp = Arrays.copyOfRange(first, i, i + second.length);
					distance = getDistance(temp, second);
					if (distance < bestDist)
						bestDist = distance;
				}
			} else {
				for (int i = 0; i < (second.length - first.length) + 1; i++) {

					double[] temp = Arrays.copyOfRange(second, i, i + first.length);
					distance = getDistance(temp, first);

					if (distance < bestDist)
						bestDist = distance;
				}
			}
		}

		return bestDist;
	}

	/**
	 * Returns squared Euclidean distance between two series of equal length.
	 * 
	 * @param first
	 *            The first series.
	 * @param second
	 *            The second series.
	 * @return The Euclidean distance between the series.
	 */
	private double getDistance(double[] first, double[] second) {
		double distance = 0;
		for (int i = 0; i < first.length; i++)
			distance = distance + ((first[i] - second[i]) * (first[i] - second[i]));

		return Math.sqrt(distance);
	}

	/**
	 * Rebuilds distance map from scratch - not efficient.
	 * 
	 * @return The adjusted distance map.
	 */
	private double[][] adjustDistanceMap() {
		double[][] map = new double[clusterPairs.size()][];

		// Initialise distance map
		for (int i = 0; i < map.length; i++) {
			double[] tmp = new double[clusterPairs.size()];
			map[i] = tmp;
		}

		// Retrieve distances from original distance map.
		for (int i = 0; i < clusterPairs.size(); i++) {

			map[i][i] = 0;

			for (int j = i + 1; j < clusterPairs.size(); j++) {
				map[i][j] = averageDistance(clusterPairs.get(i), clusterPairs.get(j));
				map[j][i] = map[i][j];
			}
		}

		return map;
	}

	/**
	 * Returns the average distance for the distance map.
	 * 
	 * @param first
	 *            First cluster.
	 * @param second
	 *            Second cluster.
	 * @return Average distance.
	 */
	private double averageDistance(int[] first, int[] second) {
		double dist = 0;

		for (int i = 0; i < first.length; i++) {
			for (int j = 0; j < second.length; j++) {
				dist = dist + distanceMap[first[i]][second[j]];
			}
		}

		dist = dist / (first.length * second.length);

		return dist;
	}

	//

	/**
	 * Takes a pair of indexes to the clusterPair ArrayList and merges the
	 * entries.
	 * 
	 * @param pair
	 *            A pair of indexes to the clusterPair ArrayList.
	 */
	private void adjustClusterPairs(int[] pair) {
		int[] first = clusterPairs.get(pair[0]);
		int[] second = clusterPairs.get(pair[1]);

		int[] tmp = new int[first.length + second.length];

		for (int i = 0; i < tmp.length; i++) {
			if (i < first.length) {
				tmp[i] = first[i];
			} else {
				tmp[i] = second[i - first.length];
			}
		}

		clusterPairs.remove(pair[0]);
		clusterPairs.add(pair[0], tmp);
		clusterPairs.remove(pair[1]);

	}

	/**
	 * Returns the noClust variable.
	 * 
	 * @return noClust.
	 */
	public int getNoClust() {
		return this.noClust;
	}

	/**
	 * Sets the number of clusters to use.
	 * 
	 * @param num
	 *            The number of clusters.
	 */
	public void setNoClust(int num) {
		this.noClust = num;
	}

	public void setShapeletTransform(ShapeletTransform s) {
		st = s;
	}

	@Override
	public String globalInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
