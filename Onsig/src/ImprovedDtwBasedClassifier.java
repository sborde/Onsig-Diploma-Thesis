
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import signature.Signature;
import training.ImprovedTrainingSet;
import evaluate.ImprovedDTWClassifier;

/**
 * A továbbfejlesztett DTW algoritmust futtató 
 * tanító és kiértékelő osztály, a program 
 * belépési pontja.
 * @author sborde
 *
 */
public class ImprovedDtwBasedClassifier {
	
	/**
	 * Validáló halmaz aránya. A könnyebb osztás miatt
	 * az tárolom, hogy hány részre kell osztani
	 * a tanítóhalmazt (és ebből mindig egy rész
	 * lesz a teszthalmaz).
	 */
	private static int testSetRate = 2;
	
	/**
	 * Felhasználandó oszlopok sorszámai.
	 */
	public static int []cols = {3,4,5};
	
	/**
	 * Az egyes koordináták súlyai.
	 */
	public static double []weights;
	
	/**
	 * A projekt főkönyvtára.
	 */
	public static String baseDir;
	
	/**
	 * Beolvas egy megadott aláírást fájlból.
	 * @param type típusa lehet valid vagy hamisítvány
	 * @param name fájl neve
	 * @return beolvasott aláírás
	 */
	private static Signature readInSignature(String type, String name) {
		double[] tempweights = {1.0,1.0,1.0};
		return new Signature("../data-deriv/" + type + "/" + name, cols.length, cols, tempweights);
	}
	
	/**
	 * Beolvassa egy felhasználóhoz tartozó összes hamisítást.
	 * @param userId felhasználó azonosítója
	 * @return hamisítványok tömbje
	 */
	private static ArrayList<Signature> readUserForgedSignatures(String userId) {
		ArrayList<Signature> userSigs = new ArrayList<Signature>();
		File forgdir = new File(baseDir + "/data-deriv/forgery/");   //eredeti fájlokat tartalmazó könyvtár
		String []forgfiles = forgdir.list();  //összes valid aláírás (minden usertől)
		java.util.Arrays.sort(forgfiles);
		
		for ( int i = 0 ; i < forgfiles.length ; i++ ) {
            if ( Pattern.matches("[0-9]*_"+userId+"_[0-9]^*[0-9]*.HWR", forgfiles[i]) )
            	userSigs.add(readInSignature("forgery", forgfiles[i]));
            
        }
				
		return userSigs;
	}
	
	/**
	 * Bizonyos aláírásokat olvasunk be a valódiak közül. A listában már a fájl nevét kapjuk.
	 * @param list fájlnevek listája
	 * @return beolvasott aláírások
	 */
	private static ArrayList<Signature> readInSpecificValidSignatures(ArrayList<String> list) {
		ArrayList<Signature> userSigs = new ArrayList<Signature>();
		
		for ( int i = 0 ; i < list.size() ; i++ ) {
            userSigs.add(readInSignature("genuine", list.get(i)));
        }
				
		return userSigs;
	}
	
	/**
	 * Elkészít egy teszthalmazmérettel elérhető összes felosztást.
	 * @param userId felhasználó azonosító
	 * @param testSetSize teszthalmaz mérete
	 * @return teszthalmaz és tanítóhalmaz aláírásainak a fájlneve
	 */
	private static ArrayList<ArrayList<ArrayList<String>>> readInTestSet(String userId, int testSetSize){
		
		/*
		 * Egy felhasználó összes felbontási lehetősége, azon belül teszt és tanítóhalmazra osztva.
		 * */
		ArrayList<ArrayList<ArrayList<String>>> output = new ArrayList<ArrayList<ArrayList<String>>>();
		
		ArrayList<String> testSet = new ArrayList<String>();
		ArrayList<String> trainSet = new ArrayList<String>();
		
		File genuinedir = new File(baseDir + "/data-deriv/genuine/");   //eredeti fájlokat tartalmazó könyvtár
		String []genuinefiles = genuinedir.list();  //összes valid aláírás (minden usertől)
		
		ArrayList<String> userFiles = new ArrayList<String>();	//csak adott user valid aláírásai
		for ( int i = 0 ; i < genuinefiles.length ; i++ ) {
            if ( Pattern.matches(userId+"_[0-9]{2,}.HWR", genuinefiles[i]) ){
            	userFiles.add(genuinefiles[i]);
            }
            
        }
		
		if ( userFiles.size() == 0 )	//ha nincs neki, akkor térjünk vissza
			return null;
		
		int permutation = userFiles.size()/testSetSize;	//kiszámítjuk, hányféle teszthalmaz felosztás lehet
		//Collections.shuffle(userFiles);	//összekeverjük, hogy véletlenszerű legyen
		
		for ( int k = 0 ; k < permutation ; k++ ) {
			ArrayList<ArrayList<String>> toOutput = new ArrayList<ArrayList<String>>();
			
			testSet = new ArrayList<String>();
			trainSet = new ArrayList<String>();
			for ( int i = 0 ; i < userFiles.size() ; i++ ) {	//beolvassuk az összes aláírást
	        
				if ( i >= k*testSetSize && i < (k+1)*testSetSize )
					testSet.add(userFiles.get(i));
				else if ( (i < k*testSetSize) || (i>=(k+1)*testSetSize) )
					trainSet.add(userFiles.get(i));
			}
			
			toOutput.add(testSet);
			toOutput.add(trainSet);
			output.add(toOutput);
		}
				
		return output;
	}
	
	/**
	 * Kideríti, hány aláírása van az adott usernek. A mintával nem rendelkezők kiszűréséhez használjuk.
	 * @param userId felhasználó azonosító
	 * @return aláírások darabszáma
	 */
	private static int getSignaturesNumber(String userId) {
		int count = 0;
		File genuinedir = new File(baseDir + "/data-deriv/genuine/");   //eredeti fájlokat tartalmazó könyvtár
		String []genuinefiles = genuinedir.list();  //összes valid aláírás (minden usertől)
		
		for ( int i = 0 ; i < genuinefiles.length ; i++ ) {
            if ( Pattern.matches(userId+"_[0-9]^*[0-9]*.HWR", genuinefiles[i]) )
            	count++;
            
        }
				
		return count;		
	}
	
	/**
	 * Kiértékeli a modellemet egy független teszthalmazon egy alanyra.
	 * @param postestSamples pozitív tesztpéldák (ismeretlen valódi aláírások)
	 * @param negtestSamples negatív tesztpéldák (ismeretlen hamisítványok)
	 * @param trainSet tanító halmaz (ismert, tanulható példák)
	 * @param k küszöbérték
	 */
	private static double[] runTest(ArrayList<String> postestSamples, ArrayList<Signature> negtestSamples, ArrayList<String> trainingSamples, double k) {

		/* Feltöltöm a halmazokat. */
		ArrayList<Signature> posTest = readInSpecificValidSignatures(postestSamples);
		ArrayList<Signature> train = readInSpecificValidSignatures(trainingSamples);
		ArrayList<Signature> negTest = negtestSamples;
		
		//létrehozom a tanítóhalmazt és betanítom
		ImprovedTrainingSet its = new ImprovedTrainingSet(weights);
		for ( Signature p : train)
			 its.addSignature(p);
		 
		its.makeTemplates();
		ImprovedDTWClassifier classifier = new ImprovedDTWClassifier(its);	//osztályozó

		int tp = 0;
		int tn = 0;
		int fp = 0;
		int fn = 0;
		
		//valódí aláírások
		for ( Signature ts : posTest ) {
			double decision = classifier.classify(ts, k);	//döntés
			if ( decision == -1 ) 
				continue;
			
			if ( decision == 0 ) 
				fn++;
			else if ( decision == 1 ) 
				tp++;

	    }
		
		//hamisítványok
		for ( Signature ts : negTest ) {
			double decision = classifier.classify(ts, k);
			if ( decision == 1 ) 
				fp++;
			else
				tn++;

		}
		
		double frr = (double)fn / (fn+tp);
		double far = (double)fp / (fp+tn);
		
		double []o ={frr,far};  
		return o;
	}
	
	public static void main(String args[]) {
		
		//paraméterként megadható az adatokat tartalmazó könyvtár útvonala
		if ( args.length == 0 ) 
			baseDir = "..";
		else
			baseDir = args[0];
		
		weights = new double[3];
		
		double k = 0.0;		//a k paraméter
		double w = 13.0; 	//a w súly értéke alapból
		double kStep = 0.1; //k lépésköze
		double wStep = 0.05; //súly lépésköze
		double startK = 3.6;//kezdő k érték
		double threshold = 0.0;	//megállapított küszöb
		double lastK = startK;	//legutóbbi k érték
		
		String testTitle = "";
		String fileName = "";
		String dirName = "./results-long/wosigmoid_w_test_firstderiv_optimalweight5/";	//kimenetek köynvtára
		
		while (true) {
			
			Map<Double, double[]> results = new TreeMap<Double, double[]>();	//eredmények
			
			if ( w > 13.0 ) {
				System.out.println("Elértük a max. súlyt");
				break;
			}
				
			w += wStep;	//léptetjük a w-t
			w = (Math.round(w*100)/100.0);	//hogy ne legyen nagyon sok tizedesjegy
			
			
			weights[0] = weights[1] = 1.0;
			weights[2] = w;
			
			fileName = "_w_"+w;
			testTitle = "Elso_derivalt_zsuly_"+w;
						
			k = startK;
			
			System.out.println("Súlyok: [" + weights[0] + "," + weights[1] + "," + weights[2] + "]");
			
			/* Az i. helyen található egy listákat tartalmazó lista. A j. lista a j. user aláírásait tartalmazza. */
			Map<Integer, ArrayList<ArrayList<String>>> trainingByIter = new HashMap<Integer, ArrayList<ArrayList<String>>>(3);
			Map<Integer, ArrayList<ArrayList<String>>> testByIter = new HashMap<Integer, ArrayList<ArrayList<String>>>(3);

			for ( int i = 1 ; i <= 16 ; i++ ) {
				String userId = "0" + i;
				if ( i < 10 )
					userId = "0" + userId;
				
				ArrayList<ArrayList<ArrayList<String>>> testAndTrains = readInTestSet(userId, 8);
				for ( int m = 0 ; m < 3 ; m++ ) {
					if ( trainingByIter.get(m) == null ) {	//ha még nem hoztunk létre erre az iterációra halmazt, tegyük meg
						trainingByIter.put(new Integer(m), new ArrayList<ArrayList<String>>(17));
						trainingByIter.get(m).add(null);	//feltöltjük a 0. helyet null-al, hogy könnyebb legyen kezelni
					}
					
					if ( testByIter.get(m) == null ) {	//ha még nem hoztunk létre erre az iterációra halmazt, tegyük meg
						testByIter.put(new Integer(m), new ArrayList<ArrayList<String>>(17));
						testByIter.get(m).add(null);	//feltöltjük a 0. helyet null-al, hogy könnyebb legyen kezelni
					}
					
					if ( testAndTrains == null ) {	//ha nincs ilyen aláírónk, akkor az aktuális iteráció helyére null-t tegyünk
						trainingByIter.get(m).add(null);
						testByIter.get(m).add(null);
						continue;
					}
				
					ArrayList<ArrayList<String>> testAndTrain = testAndTrains.get(m); 

					testByIter.get(m).add(testAndTrain.get(0));	//az m. helyen lévő listába szúrjuk be a listát
					trainingByIter.get(m).add(testAndTrain.get(1));
				}
			}
			for ( int m = 0 ; m < 3 ; m++ ) {	//végigvesszük az összes lehetséges teszthalmaz választást
				try {
					PrintWriter pw1 = new PrintWriter(new FileOutputStream(dirName+"/eer"+fileName+"_"+m+".txt"));
					PrintWriter pw2 = new PrintWriter(new FileOutputStream(dirName+"/roc"+fileName+"_"+m+".txt"));
			
					pw1.println(testTitle);
					pw2.println(testTitle);
					
					while (true) {
						
						int tp = 0;
						int tn = 0;
						int fp = 0;
						int fn = 0;
						
						k -= kStep;	//léptetjük a k-t
						k = (Math.round(k*100)/100.0);	//hogy ne legyen nagyon sok tizedesjegy
						
						if ( k < 3.0 )
							break;
	
						System.out.println("k = " + k);
						
						//vizsgáljuk meg az összes aláírót
						for ( int i = 1 ; i <= 16 ; i++ ) {
						
							
							String userId = "0" + i;
							if ( i < 10 )
								userId = "0" + userId;
				
							if ( getSignaturesNumber(userId) == 0 )	//nincs ilyen aláírónk 
								continue;
							
							int numberOfSignatures = trainingByIter.get(m).get(i).size();	//egyébként lekérjük a darabszámot
							
				
							int numberOfTrainingSet = numberOfSignatures / testSetRate;
							ArrayList<Signature> forgery = readUserForgedSignatures(userId);	//az aktuálisan vizsgált személy hamisításai
							
							//l jelöli, hogy most épp hanyadik validálást hajtjuk végre
							for ( int l = 0 ; l < testSetRate ; l++ ) {
								ArrayList<Signature> usersSig = readInSpecificValidSignatures((trainingByIter.get(m).get(i)));
								
								ImprovedTrainingSet train = new ImprovedTrainingSet(weights);	//tanítóhalmaz létrehozása
								
								ArrayList<Signature> test = new ArrayList<Signature>();	//validáló aláírások gyűjtőhelye
								
								for ( int j = 0 ; j < usersSig.size() ; j++ ) {
									Signature s = usersSig.get(j);
									s.reset();	//visszaállítjuk mindig az eredeti állapotra az aláírást
									if ( j < l*numberOfTrainingSet || j >= (l+1)*numberOfTrainingSet ) {
										train.addSignature(s);
									} else {
										test.add(s);
									}
								}
								
								train.makeTemplates();
								ImprovedDTWClassifier classifier = new ImprovedDTWClassifier(train);
								
								for ( Signature ts : test ) {
									double decision = classifier.classify(ts, k);
									if ( decision == -1 ) 
										continue;
									
									if ( decision == 0 ) {
										fn++;
									} else if ( decision == 1 ) {
										tp++;
									}
								}
								for ( Signature ts : forgery ) {
									double decision = classifier.classify(ts, k);
									if ( decision == 1 ) {
										fp++;
									} else {
										tn++;
									}
								}
							}
						}
						
						double frr = (double)fn / (fn+tp);
						double far = (double)fp / (fp+tn);
						
						double[] farr = {frr, far};
						results.put(new Double(k), farr);
						;
						
					}
					
					double index = Math.round((k+kStep)*10)/10.0;	//kiszámítjuk a legnagyobb k értéket
					double[] farrr = results.get(3.0);
					int lastSignum = (int)Math.signum(farrr[0]-farrr[1]);	//és a legelső irányt
					double lastAvg = 0.0;	//az előző esetben a két hiba átlaga (ezt tekintjük majd EER-nek)
					double EER = 0.0;		//ez lesz a közelített EER
					
					for ( Map.Entry<Double, double[]> e : results.entrySet()) {
						int signum = (int)Math.signum(e.getValue()[0] - e.getValue()[1]);	//melyik a nagyobb
						double avg = (e.getValue()[0] + e.getValue()[1])/2;					//a két hiba átlaga
						if ( signum != lastSignum ) {	//ha megfordult az egyenlőtlenség iránya, akkor itt az EER (magában foglalja az egyenlőséget)
							EER = (lastAvg + avg) / 2;	// a két szomszédos EER átlaga
							threshold = (e.getKey()+lastK)/2;
							pw1.println("Úgy találtam, hogy az EER " + EER + " az ehhez tartozó threshold pedig " + threshold);
							System.out.println("Úgy találtam, hogy az EER " + EER + " az ehhez tartozó threshold pedig " + threshold);
						}
						lastSignum = (int)Math.signum(e.getValue()[0] - e.getValue()[1]);
						lastAvg = avg;
						lastK = e.getKey();
						pw1.println(e.getKey() + " " + e.getValue()[0] + " " + e.getValue()[1]);
						System.out.println(e.getKey() + " " + e.getValue()[0] + " " + e.getValue()[1]);
					}
					threshold = 3.05; 
					PrintWriter pw = new PrintWriter(new FileOutputStream(dirName+"/"+w+"_test_error_"+m+".txt"));
					for ( int i = 1 ; i <= 16 ; i++ ) {
						String userId = "0" + i;
						if ( i < 10 )
							userId = "0" + userId;
						
						if ( testByIter.get(m).get(i) == null )
							continue;
						
						double []error = runTest(testByIter.get(m).get(i), readUserForgedSignatures(userId), trainingByIter.get(m).get(i), threshold);
						pw.println(error[0] + " " + error[1]);
					}

					pw.flush();
					pw1.flush();
					pw2.flush();
					pw.close();
					pw1.close();
					pw2.close();
				} catch (IOException e ){
					e.printStackTrace();
				}
			}
		}
	}
	
}
