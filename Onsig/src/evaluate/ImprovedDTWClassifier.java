package evaluate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import signature.*;
import training.*;

public class ImprovedDTWClassifier {
	
	private ImprovedTrainingSet trainingSet;

	public ImprovedDTWClassifier(ImprovedTrainingSet trainingSet) {
		this.trainingSet = trainingSet;
	}
	
	public double classify(signature.Signature s,double k) {
		double distance = trainingSet.calcDistanceFrom(s);
		double avgDist = trainingSet.getAverageDistance();
		double avgDev = trainingSet.getDistanceDeviation();
		double threshold = avgDist + avgDev * k;
		
		if ( distance == Double.MAX_VALUE )
			return -1;
		//ha a távolság nagyobb a küszöbértéknél, akkor elutasítjuk az aláírást
		if ( distance > threshold )
			return 0;
		
		return 1;
	}
	
	
}