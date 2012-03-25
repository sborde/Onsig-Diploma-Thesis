package evaluate;

import signature.*;
import training.*;

public class FastDTWClassifier {
	
	private ImprovedTrainingSet trainingSet;

	public FastDTWClassifier(ImprovedTrainingSet trainingSet) {
		this.trainingSet = trainingSet;
	}
	
	public boolean classify(signature.Signature s,double k) {
		double distance = trainingSet.calcDistanceFrom(s);
		double avgDist = trainingSet.getAverageDistance();
		double avgDev = trainingSet.getDistanceDeviation();
		double threshold = avgDist + avgDev * k;
		//ha a távolság nagyobb a küszöbértéknél, akkor elutasítjuk az aláírást
		if ( distance > threshold )
			return false;
		
		return true;
	}
	
	
}
