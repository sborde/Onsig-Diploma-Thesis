package evaluate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import signature.*;
import training.*;

/**
 * A továbbfejlesztett DTW algoritmus segítségével hoz egy döntést.
 * Tartalmazza a tanítóhalmazt, amir alapján választ. A küszöbértéket
 * döntéskor paraméterként kapja.
 * @author sborde
 *
 */
public class ImprovedDTWClassifier {
	
	/**
	 * Tanítóhalmaz.
	 */
	private ImprovedTrainingSet trainingSet;

	/**
	 * A tanítóhalmazt kapjuk értékül és azt eltároljuk.
	 * @param trainingSet tanítóhalmaz
	 */
	public ImprovedDTWClassifier(ImprovedTrainingSet trainingSet) {
		this.trainingSet = trainingSet;
	}
	
	/**
	 * A döntést meghozó metódus. Kap egy aláírást, melyről véleményt
	 * kell mondani, valamint egy k küszöbértéket, mely a toleranciaszintet
	 * állítja be. Kimenetben egy osztálycímkét ad. -1-et akkor vehet fel,
	 * ha egyből elutasítottuk az aláírást, mert nem volt megfelelő
	 * szakaszszám.
	 * @param s vizsgált aláírás
	 * @param k küszöbérték
	 * @return osztálycímke
	 */
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