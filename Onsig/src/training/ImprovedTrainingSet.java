package training;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import signature.*;

public class ImprovedTrainingSet {
	
	/**
	 * Egy aláíróhoz tartozó összes aláírás.
	 */
	ArrayList<Signature> signatures;
	
	/**
	 * Aláírások szakaszok száma szerint szétválogatva.
	 */
	Map<Integer, ArrayList<Signature>> sigsByStrokeNum;
	
	/**
	 * Eltárolja az egyes aláírások távolságát a hozzá
	 * tartozó templatetől.
	 */
	Map<Integer, ArrayList<Double>> sigDistanceByStrokeNum;
	
	/**
	 * Template aláírások szakaszok száma szerint indexelve.
	 */
	Map<Integer, TemplateSignature> templates;
	
	/**
	 * Az egyes koordináták súlyai a template számításkor.
	 */
	private double[] coordWeights;
	
	/**
	 * Az aláírások átlagos eltérése a saját templatejüktől.
	 */
	private double averageDistance;
		
	/**
	 * Az aláírások távolságának átlagos szórása a saját templatejüktől.
	 */
	private double distanceDeviation;

	public double getAverageDistance() {
		return averageDistance;
	}

	public double getDistanceDeviation() {
		return distanceDeviation;
	}

	public ImprovedTrainingSet(double[] coordWeights) {
		signatures = new ArrayList<Signature>();
		templates = new HashMap<Integer, TemplateSignature>();
		sigsByStrokeNum = new HashMap<Integer, ArrayList<Signature>>();
		this.coordWeights = coordWeights;
	}
	
	/**
	 * Aláírás objektumokat beteszi a tanítóhalmazba.
	 * @param sigs aláírások
	 */
	public void addSignatures(ArrayList<Signature> sigs) {
		this.signatures = sigs;
	}
	
	/**
	 * Egy konkrét aláírást tesz be a halmazba, valamint beteszi a megfelelő
	 * szakasz-számú listába is.
	 * @param s betenni kívánt aláírás
	 */
	public void addSignature(Signature s) {
		/* Csak egyszer szerepeljen mindegyik. */
		if ( !signatures.contains(s) ) 
			signatures.add(s);
		
		putToSegmentedList(s);
	}
	
	/**
	 * A paraméterben kapott aláírást beteszi a szakaszok száma szerinti listába.
	 * @param s válogatni kívánt aláírás
	 */
	private void putToSegmentedList(Signature s) {
		Integer strokenum = s.numberOfSegments();	//aláírás szegmenseinek száma
		
		//Ennyi szegmensből álló aláírások halmaza
		if ( !sigsByStrokeNum.containsKey(strokenum) ) 
			sigsByStrokeNum.put(strokenum, (new ArrayList<Signature>()));
		
		/* Itt is csak egyszer szerepeljen. */
		if ( !sigsByStrokeNum.get(strokenum).contains(s) )
			sigsByStrokeNum.get(strokenum).add(s);
	}
	
	/**
	 * Az aláírások ömlesztett listáját szétválogatja szakaszok száma szerint.
	 */
	private void sortSignatures() {
		for ( Signature s : signatures ) {			
			putToSegmentedList(s);
		}
	}
	
	/**
	 * Elkészíti minden szakasz számú aláíráshoz tartozó template aláírást.
	 */
	public void makeTemplates() {
			Set<Integer> strokeNumbers = sigsByStrokeNum.keySet();	//lekérjük a szakasz darabszámokat
			for ( Integer sn : strokeNumbers ) {
				
				ArrayList<Signature> signs = sigsByStrokeNum.get(sn);	//lekérjük az ennyi szakaszból álló aláírásokat
				int[] strokeAverageLength = new int[sn];	//ebben tároljuk majd az egyes szakaszok átlag hosszát
				
				for ( Signature sig : signs )	//minden aláírásra
					for ( int i = 0 ; i < sn ; i++ )	//minden szakaszra
						strokeAverageLength[i] += sig.getSegment(i).size();
					
				for ( int i = 0 ; i < sn ; i++ ) 
					strokeAverageLength[i] /= signs.size();
				
				/* 
				 * Az első aláírást még itt újramintavételezzük az átlagos szakaszhosszok alapján.
				 * A többit már a templatekészítéskor fogjuk ez alapján.
				 */
				for ( int i = 0 ; i < sn ; i++ )	//minden szakaszra
					signs.get(0).resampleSegment(i, strokeAverageLength[i]);
				
				TemplateSignature t = new TemplateSignature(this.coordWeights);
				for ( Signature sig : signs )	//minden aláírásra
					t.addSignature(sig);
				
				this.templates.put(sn, t);	//végül betesszük, hogy könnyen kereshető legyen
				t.calculatePointConsistency();
				t.calculatePointWeights();
			} 
			
			calcAverageDistance();
			calcDistanceDeviation();
	}
	
	/**
	 * Kiszámítja a tanítóhalmaz aláírásai és a hozzájuk tartozó sablon közötti távolságok
	 * szórását. A szórás az átlagtól vett eltérés négyzetösszege leosztva az elemek számával.
	 */
	public void calcDistanceDeviation() {
		double negyzetosszeg = 0.0;
		Set<Integer> k = this.sigsByStrokeNum.keySet();
		for ( Integer k1 : k ) {
			for ( Double d : this.sigDistanceByStrokeNum.get(k1) ) {
				negyzetosszeg += Math.pow(d-averageDistance, 2);
			}
		}
		negyzetosszeg /= this.signatures.size();
		this.distanceDeviation = Math.sqrt(negyzetosszeg);
	}
	
	/**
	 * Kiszámítja, hogy a tanító aláírások milyen távol vannak átlagosan
	 * a hozzájuk tartozó sablontól. A döntéshez használjuk majd fel.
	 */
	public void calcAverageDistance() {
		this.sigDistanceByStrokeNum = new HashMap<Integer, ArrayList<Double>>();
		averageDistance = 0.0;
		
		Set<Integer> k = this.sigsByStrokeNum.keySet();
		
		for ( Integer k1 : k ) {
			this.sigDistanceByStrokeNum.put(k1, new ArrayList<Double>());
			ArrayList<Signature> sigs2 = this.sigsByStrokeNum.get(k1);
			
			for ( Signature s : sigs2 ) {
				s.reset();	//visszaállítjuk eredeti állapotába az aláírást
				double dist = this.calcDistanceFrom(s);
				this.sigDistanceByStrokeNum.get(k1).add(dist);
				averageDistance += dist;
			}
		}
		
		averageDistance /= this.signatures.size();
	}
	
	/**
	 * Kiszámítja egy kapott aláírás és a hozzá tartozó sablon aláírás
	 * távolságát. Ha nincs neki megfelelő sablon (tehát más a szakaszszáma,
	 * mint bármelyik sablonnak), akkor végtelen a távolság.
	 * @param s vizsgálandó aláírás
	 * @return távolság
	 */
	public double calcDistanceFrom(Signature s) {
		double distance = 0.0;
		int testSignatureSegmentCount = s.numberOfSegments();
		/* Ha nincs ennyi szakaszból álló aláírás, akkor nincs több dolgunk, elutasítjuk. */
		if ( !this.templates.containsKey(testSignatureSegmentCount) ) 
			return Double.MAX_VALUE;
		
		int testSignaturePenDownTime = s.getTotalPenDownTime();	//lekérjük még újramintavételezés előtt az egyes időket
		int testSignaturePenUpTime = s.getTotalPenUpTime();
		int testSignatureTotalTime = s.getTotalTime();
	          
		//Szegmensenként kell számítani a távolságot, majd a végén ezeket összegezni.
		for ( int i = 0 ; i < testSignatureSegmentCount ; i++ ) {
			if ( !s.isPendownSegment(i) )	//az algoritmusunk csak az író szakaszokat veszi figyelembe
				continue;
			
			TemplateSignature template = this.templates.get(testSignatureSegmentCount);	//aktuális template, amihez hasonlítok
			int newLength = template.getSegment(i).size();	//szegmens mérete, ami szerint újra kell mintavételezni
			
			s.resampleSegment(i, newLength);	//újramintavételezzük a kapott aláírást
			double segmentDistance = com.dtw.ImprovedDTW.getWarpDistBetween(template.getSegment(i), s.getSegments().get(i), this.coordWeights, template.getSegmentPointWeightsArray(i));	//a szegmens távolsága
			double factor = (Math.abs(testSignatureTotalTime-template.getTotalTime())) / (double)template.getTotalTime();	//szegmens távolsága az írási idők figyelembe vételével
			
			segmentDistance *= (1+factor);
			
			distance += segmentDistance;	//az össz távolságot frissítjük
		}
		return distance;
	}
	
	
	
}
