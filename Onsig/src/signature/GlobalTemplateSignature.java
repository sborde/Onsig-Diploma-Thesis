package signature;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.dtw.WarpPath;

public class GlobalTemplateSignature extends Signature {

	/**
	 * Azonos aláíróhoz aláírások. Ezekből készül a 
	 * template.
	 */
	private ArrayList<Signature> signatures;
	
	/**
	 * A mintához igazított aláírások igazítási útvonalait 
	 * tárolja, szegmensenként. A i. elem j. útvonala az i. igazított
	 * minta j. szegmensének igazítása.
	 */
	private ArrayList<WarpPath> segmentsPaths;
	
	/**
	 * Annak a száma, hogy az előző aláírás illesztése során az
	 * i. szegmens j. pontjához hány másikat rendeltünk.
	 */
	private ArrayList<Integer> r;
	
	/**
	 * A template pontjainak átlagos szórása.
	 */
	private ArrayList<Double> avgd;
	
	/**
	 * Az egyes koordináták súlyai a távolság számításkor.
	 */
	private double[] coordWeights;
	
	/**
	 * Tesztaláírás számításakor az egyes pontok súlya (ami a konzisztenciájukból adódik).
	 * Szegmensenként számoljuk.
	 */
	private double[] pointWeights;
	
	/**
	 * A maximális pont szórás. A normalizáláshoz fog kelleni.
	 */
	private double maxDev;
	
	
	
	public double[] getPointWeightsArray() {
		return pointWeights;
	}

	public GlobalTemplateSignature(double[] weights) {
		super();
		signatures = new ArrayList<Signature>();
		this.coordWeights = weights;
		this.segmentsPaths = new ArrayList<WarpPath>();
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
		r = new ArrayList<Integer>(s.getWholeSignature().size());
		
		for ( int j = 0 ; j < s.getWholeSignature().size() ; j++ ) {
			r.add(j, 1);
		}
	}
	
	/**
	 * A template frissítését hajtja végre.
	 * @param lastIndex a legutóbb betett aláírás indexe (ezzel kell frissíteni)
	 * @param s az utolsóként betett aláírás 
	 */
	private void update(int lastIndex, Signature s){
			for ( int j = 0 ; j < wholeSignature.size(); j++ ) {	//végigvesszük a szegmens pontjait
				int dim = wholeSignature.getMeasurementVector(0).length;	//hány dimenziós pontokkal dolgozunk
				ArrayList<Integer> alignedPoints = segmentsPaths.get(lastIndex).getMatchingIndexesForI(j);	//a j. ponthoz igazított pontok indexei
				
				int k = alignedPoints.size();	//az aktuális aláírás illesztése során használt pontok száma
				for ( int d = 0 ; d < dim ; d++ ) {	//dimenziónként frissítjük a pont értékét
					double templateD = wholeSignature.getMeasurement(j, d) * r.get(j);	//az eddigi érték súlyozva
					
					for (Integer t : alignedPoints) {	//minden illesztett ponttal elvégezzük a frissítést
						templateD += s.getWholeSignature().getMeasurement(t, d);	//hozzáadjuk az illesztett pont d. koordinátáját
					}
					
					templateD /= (r.get(j) + k);	//leosztjuk az összes illesztett pont számával (az átlagolás miatt)
					this.wholeSignature.setMeasurement(j, d, templateD);	//végül frissítjük az új értékkel a pontot
				}
				r.set(j, (r.get(j)+k) );	//frissítjük az r értékét
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
					
			s.resampleWholeSignature(this.wholeSignature.size());	//újramintavételezzük az adott szakaszt
			
			WarpPath wp = com.dtw.FastImprovedDTW.getWarpPathBetween(wholeSignature, s.getWholeSignature(), coordWeights);
			segmentsPaths.add(currentSignatureIndex, wp);

			update(currentSignatureIndex, s);
		}
		signatures.add(s);
		
	}
	
	/**
	 * Kiszámítja a sablon aláírás egyes pontjainak átlagos szórását (tehát a hozzá illesztett pontoktól 
	 * vett távolságának átlagát). Ezt használhatjuk a pont konzisztenciájának mérésére.
	 */
	public void calculatePointConsistency() {
		this.avgd = new ArrayList<Double>(wholeSignature.size());
		this.maxDev = 0;

		for ( int j = 0 ; j < this.wholeSignature.size() ; j++ ) {	//az aláírás pontjain
			double avgd = 0;
			double M = 0;
			for ( int t = 0 ; t < this.segmentsPaths.size() ; t++ ) {	//mindegyik aláíráson
				WarpPath wp = segmentsPaths.get(t);
				if ( wp.getMatchingIndexesForI(j).size() == 0 )
					System.out.println("Üres");
				
				ArrayList<Integer> alignedPoints = segmentsPaths.get(t).getMatchingIndexesForI(j);
				
				M += alignedPoints.size();
				for ( Integer p : alignedPoints ) {
					avgd += com.dtw.ImprovedDTW.getLocalCost(this.wholeSignature, signatures.get(t).getWholeSignature(), j, p, coordWeights);	//ez a szumma alatti terület
				}
			}
			
			if ( M == 0 ) {	//ha csak egy mintánk volt egy sablonhoz, akkor nem illesztettünk semmit hozzá (azaz de, csak 0 távolsággal) tehát minden pontja egyformán konzisztens
				this.avgd.add(j, 0.0);
			}
			else {
				avgd /= M;
				this.avgd.add(j,avgd);
				if ( j > 0 ) {
					double delta = Math.abs(avgd-this.avgd.get(j-1));
					if ( this.maxDev < delta )
						maxDev = delta;
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
		this.pointWeights = new double[wholeSignature.size()];

		for ( int j = 0 ; j < avgd.size() ; j++ ) {	//és a szegmens összes pontját
			if ( j == 0 ) {
				this.pointWeights[j] = 1.0;	//az első pont nem változott az előtte lévőhöz képest
				continue;
			}
			
			double delta = Math.abs(this.avgd.get(j) - this.avgd.get(j-1));	//a két szórás különbsége
			//System.out.println(delta + " " + this.maxDev[i] + " " + (delta/maxDev[i]));
			
			if ( this.maxDev == 0 )
				this.pointWeights[j] = 1.0;	//ha 0 a legnagyobb szórás, akkor nagyon konzisztensnek vesszük a pontot (mivel valószínű hogy csak egy aláírásból készült a template)
			else
				this.pointWeights[j] = sigmoid(delta,maxDev,1.2);
				//this.pointWeights[j] = linear(delta,maxDev);

		}
	}


	public static double linear(double x,double normv) {
		return (x/-normv)+1.0;
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
