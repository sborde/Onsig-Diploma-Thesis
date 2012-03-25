/**
 * 
 */
package signature;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.dtw.WarpPath;

/**
 * @author sborde
 *
 */
public class TemplateSignature extends Signature {

	/**
	 * Azonos vonalszámból álló aláírások. Ezekből készül a 
	 * template.
	 */
	private ArrayList<Signature> signatures;
	
	/**
	 * A mintához igazított aláírások igazítási útvonalait 
	 * tárolja, szegmensenként. A i. elem j. útvonala az i. igazított
	 * minta j. szegmensének igazítása.
	 */
	private ArrayList<ArrayList<WarpPath>> segmentsPaths;
	
	/**
	 * Annak a száma, hogy az előző aláírás illesztése során az
	 * i. szegmens j. pontjához hány másikat rendeltünk.
	 */
	private ArrayList<ArrayList<Integer>> r;
	
	/**
	 * A template pontjainak átlagos szórása.
	 */
	private ArrayList<ArrayList<Double>> avgd;
	
	/**
	 * Az egyes koordináták súlyai a távolság számításkor.
	 */
	private double[] coordWeights;
	
	/**
	 * Tesztaláírás számításakor az egyes pontok súlya (ami a konzisztenciájukból adódik).
	 * Szegmensenként számoljuk.
	 */
	private ArrayList<double[]> pointWeights;
	
	/**
	 * Szegmensenként a maximális pont szórás. A normalizáláshoz fog kelleni.
	 */
	private double[] maxDev;
	
	
	
	public double[] getSegmentPointWeightsArray(int i) {
		return pointWeights.get(i);
	}

	public TemplateSignature(double[] weights) {
		super();
		signatures = new ArrayList<Signature>();
		this.coordWeights = weights;
		this.segmentsPaths = new ArrayList<ArrayList<WarpPath>>();
	}
	
	/**
	 * Amikor az első aláírást vizsgáljuk adott vonalszámmal, akkor a 
	 * sablon meg fog egyezni ezzel az aláírással, tehát akkor csak simán másoljuk.
	 * @param s másolandó aláírás
	 */
	private void init(Signature s) {
		this.fileName = s.fileName;
		this.wholeSignature = s.getWholeSignature();
		this.segments = s.getSegments();
		this.segmentPenDown = s.getSegmentPenDown();
		this.totalPenDownTime = s.getTotalPenDownTime();
		this.totalPenUpTime = s.getTotalPenUpTime();
		
		/*
		 * Az r értékek inicializálása. Az első aláírásnál még 
		 * nem illesztettünk egy pontot sem másikhoz, ezért 
		 * 1 értékeket adunk neki.
		 */
		r = new ArrayList<ArrayList<Integer>>(s.numberOfSegments());
		for ( int i = 0 ; i < s.numberOfSegments() ; i++ ) {
			r.add(i,new ArrayList<Integer>(s.getSegment(i).size()));
			for ( int j = 0 ; j < s.getSegment(i).size() ; j++ ) {
				r.get(i).add(j, 1);
			}
		}
	}
	
	/**
	 * A template frissítését hajtja végre.
	 * @param lastIndex a legutóbb betett aláírás indexe (ezzel kell frissíteni)
	 * @param s az utolsóként betett aláírás 
	 */
	private void update(int lastIndex, Signature s){
		for ( int i = 0 ; i < this.segmentsPaths.get(lastIndex).size(); i++ ) {	//bejárjuk az illesztési útvonalakat
			if ( segmentsPaths.get(lastIndex).get(i) == null)	//a kitöltő elemeket átugorjuk 
				continue;
			
			for ( int j = 0 ; j < this.segments.get(i).size(); j++ ) {	//végigvesszük a szegmens pontjait
				int dim = segments.get(i).getMeasurementVector(j).length;	//hány dimenziós pontokkal dolgozunk
				ArrayList<Integer> alignedPoints = segmentsPaths.get(lastIndex).get(i).getMatchingIndexesForI(j);	//a j. ponthoz igazított pontok indexei
				
				int k = alignedPoints.size();	//az aktuális aláírás illesztése során használt pontok száma
				for ( int d = 0 ; d < dim ; d++ ) {	//dimenziónként frissítjük a pont értékét
					double templateD = this.segments.get(i).getMeasurement(j, d) * r.get(i).get(j);	//az eddigi érték súlyozva
					
					for (Integer t : alignedPoints) {	//minden illesztett ponttal elvégezzük a frissítést
						
						templateD += s.getSegment(i).getMeasurement(t, d);	//hozzáadjuk az illesztett pont d. koordinátáját
					}
					
					templateD /= (r.get(i).get(j) + k);	//leosztjuk az összes illesztett pont számával (az átlagolás miatt)
					this.segments.get(i).setMeasurement(j, d, templateD);	//végül frissítjük az új értékkel a pontot
				}
				r.get(i).set(j, (r.get(i).get(j)+k) );	//frissítjük az r értékét
			}
		}		
	}
	
	/**
	 * Egy új aláírást ad hozzá, mellyel finomíthatja a sablont. 
	 * @param s hozzáadandó aláírás
	 */
	public void addSignature(Signature s) {
		if ( signatures.size() == 0 ) {
			//Első aláírás
			init(s);
		} else {
			int currentSignatureIndex = signatures.size()-1;	//ez az iteráció sorszáma, önmagával nem kell igazítani, ezért a -1

			segmentsPaths.add(currentSignatureIndex, new ArrayList<WarpPath>());
			if ( this.segments.size()==0 )
				System.out.println("Nincs szegmens??");
			for ( int i = 0 ; i < this.segments.size() ; i++ ) {	//végigmegyünk az új aláíráson szegmensenként
				if ( !this.isPendownSegment(i) ) {	//csak az írási szakaszokkal törődünk
					segmentsPaths.get(currentSignatureIndex).add(null);	//hogy megegyezzen a szegmensek száma (az egyszerűbb bejárás miatt) adunk null útvonalat is
					continue;
				}
						
				s.resampleSegment(i, this.segments.get(i).size());	//újramintavételezzük az adott szakaszt
				
				WarpPath wp = com.dtw.FastImprovedDTW.getWarpPathBetween(segments.get(i), s.getSegment(i), coordWeights);
				segmentsPaths.get(currentSignatureIndex).add( wp );	//hozzáadjuk az adott szegmens útvonalát
			}
			
			update(currentSignatureIndex, s);
		}
		signatures.add(s);
		
	}
	
	/**
	 * Kiszámítja a sablon aláírás egyes pontjainak átlagos szórását (tehát a hozzá illesztett pontoktól 
	 * vett távolságának átlagát). Ezt használhatjuk a pont konzisztenciájának mérésére.
	 */
	public void calculatePointConsistency() {
		this.avgd = new ArrayList<ArrayList<Double>>(this.segments.size());
		this.maxDev = new double[this.segments.size()];
		for ( int i = 0 ; i < this.segments.size() ; i++ ) {	//menjünk végig szegmensenként a template aláíráson
			if ( !this.isPendownSegment(i)) {
				this.avgd.add(i,null);
				continue;
			}
		
			this.avgd.add(i, new ArrayList<Double>(this.segments.get(i).size()));
			for ( int j = 0 ; j < this.segments.get(i).size() ; j++ ) {	//szegmensek pontjain
				double avgd = 0;
				double M = 0;
				for ( int t = 0 ; t < this.segmentsPaths.size() ; t++ ) {	//mindegyik aláíráson
					WarpPath wp = segmentsPaths.get(t).get(i);
					if ( wp.getMatchingIndexesForI(j).size() == 0 )
						System.out.println("Üres");
					ArrayList<Integer> alignedPoints = segmentsPaths.get(t).get(i).getMatchingIndexesForI(j);
					
					M += alignedPoints.size();
					for ( Integer p : alignedPoints ) {
						avgd += com.dtw.ImprovedDTW.getLocalCost(this.segments.get(i), signatures.get(t).getSegment(i), j, p, coordWeights);	//ez a szumma alatti terület
					}
				}
				
				if ( M == 0 ) {	//ha csak egy mintánk volt egy sablonhoz, akkor nem illesztettünk semmit hozzá (azaz de, csak 0 távolsággal) tehát minden pontja egyformán konzisztens
					this.avgd.get(i).add(j, 0.0);
				}
				else {
					avgd /= M;
					this.avgd.get(i).add(j,avgd);
					if ( j > 0 ) {
						double delta = Math.abs(avgd-this.avgd.get(i).get(j-1));
						if ( this.maxDev[i] < delta )
							maxDev[i] = delta;
					}
				}
			}
			
		}
		
	}
	
	/**
	 * Kiszámítja az átlagos szórások alapján a pontok súlyait. Azt veszi figyelembe,
	 * hogy egy pont szórása az őt megelőzőhöz képest mennyire változott. A változás
	 * normalizált értékét leolvassuk egy fordított szigmoid függvényről, és ezt lesz a súlya.
	 * Tehát minél kisebb az eltérés az előző ponttól (tehát minél hasonlóbb a szomszédok 
	 * szórása), annál nagyobb súllyal vesszük figyelembe az adott pontot.
	 */
	public void calculatePointWeights() {
		this.pointWeights = new ArrayList<double[]>();
		for ( int i = 0 ; i < avgd.size() ; i++ ) {	//végigvesszük az összes szegmenst
			if ( !this.isPendownSegment(i)) {
				this.pointWeights.add(null);
				continue;
			}
			this.pointWeights.add(new double[avgd.get(i).size()]);
			for ( int j = 0 ; j < avgd.get(i).size() ; j++ ) {	//és a szegmens összes pontját
				if ( j == 0 ) {
					this.pointWeights.get(i)[j] = 1.0;	//az első pont nem változott az előtte lévőhöz képest
					continue;
				}
				
				double delta = Math.abs(this.avgd.get(i).get(j) - this.avgd.get(i).get(j-1));	//a két szórás különbsége
				//System.out.println(delta + " " + this.maxDev[i] + " " + (delta/maxDev[i]));
				
				if ( this.maxDev[i] == 0 )
					this.pointWeights.get(i)[j] = 0.0;	//ha 0 a legnagyobb szórás, akkor nagyon konzisztensnek vesszük a pontot (mivel valószínű hogy csak egy aláírásból készült a template)
				else
					this.pointWeights.get(i)[j] = sigmoid(delta,this.maxDev[i],1.2);
				
				//System.out.println(this.pointWeights.get(i)[j]);
				
			}
		}
	}
	
	/**
	 * Ellenőrzési céllal kiírathatjuk a sablon aláírást fájlba.
	 * @param filename a célfájl neve
	 */
	public void printToFile(String filename) {
			try {
				PrintWriter file = new PrintWriter(new FileOutputStream(filename));
				for ( int i = 0 ; i < this.segments.size() ; i++ ) {
					if ( !this.isPendownSegment(i) )
						continue;
					
					for ( int j = 0 ; j < this.segments.get(i).size() ; j++ ) {
						double[] point = this.segments.get(i).getMeasurementVector(j);
						file.println(point[0] + " " + point[1] + " " + point[2]);
					}
				}
				file.flush();
				file.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	}

	public static double linear(double x) {
		return (-0.05*x)+1.0;
	}
	
	/**
	 * Egy csökkenő szigmoid függvény, amit a súlyozáshoz használhatunk.
	 * Minél kisebb az ugrás, annál jobb az érték, tehát a 0 legyen 1. 
	 * @param x függvény hely
	 * @return szigmoid értéke
	 */
	public static double sigmoid(double x) {
			 return (1/( 1 + (Math.pow(Math.E,(1*(x-0.5))))));
	}
	
	public static double sigmoid(double x, double normV, double sharpness) {
		  x=(x/normV*2-1)*5*sharpness;
		  return 1.0 / (1.0 + Math.exp(x));
	}
	
	
}
