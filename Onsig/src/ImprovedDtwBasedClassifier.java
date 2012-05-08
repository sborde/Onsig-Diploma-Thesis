
import java.awt.font.NumericShaper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import evaluate.ImprovedDTWClassifier;

import signature.*;
import training.ImprovedTrainingSet;

public class ImprovedDtwBasedClassifier {

	private static boolean DEBUG = false;
	
	/**
	 * Teszthalmaz aránya. A könnyebb osztás miatt
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
	
	public static String baseDir;
	
	/**
	 * Beolvas egy megadott aláírást fájlból.
	 * @param type típusa lehet valid vagy hamisítvány
	 * @param name fájl neve
	 * @return beolvasott aláírás
	 */
	private static Signature readInSignature(String type, String name) {
		if ( DEBUG )
			System.out.println(name + " beolvasva");
		double[] tempweights = {1.0,1.0,1.0};
		return new Signature("../data-deriv/" + type + "/" + name, cols.length, cols, tempweights);
	}
	
	/**
	 * Beolvassa a paraméterben kapott számú felhasználó valós aláírásait.
	 * @param userId felhasználó ID-je
	 * @return beolvasott aláírások listája
	 */
	private static ArrayList<Signature> readUserValidSignatures(String userId) {
		ArrayList<Signature> userSigs = new ArrayList<Signature>();
		File genuinedir = new File(baseDir + "/data-deriv/genuine/");   //eredeti fájlokat tartalmazó könyvtár
		String []genuinefiles = genuinedir.list();  //összes valid aláírás (minden usertől)
		java.util.Arrays.sort(genuinefiles);
		
		for ( int i = 0 ; i < genuinefiles.length ; i++ ) {
            if ( Pattern.matches(userId+"_[0-9]{2,}.HWR", genuinefiles[i]) ){
            	userSigs.add(readInSignature("genuine", genuinefiles[i]));
            	
            }
            
        }
				
		return userSigs;
	}
	
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
		
		if ( userFiles.size() == 0 )
			return null;
		
		int permutation = userFiles.size()/testSetSize;
		
		for ( int k = 0 ; k < permutation ; k++ ) {
			ArrayList<ArrayList<String>> toOutput = new ArrayList<ArrayList<String>>();
			Collections.shuffle(userFiles);	//összekeverjük, hogy véletlenszerű legyen
			
			for ( int i = 0 ; i < testSetSize ; i++ ) {	//beolvassuk az első n. darab aláírást a kevert listából
	            testSet.add(userFiles.get(i));
	        }
			for ( int i = testSetSize ; i < userFiles.size() ; i++ ) {
				trainSet.add(userFiles.get(i));
			}
			
			toOutput.add(testSet);
			toOutput.add(trainSet);
			output.add(toOutput);
		}
				
		return output;
	}
	
	
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
	 * @param postestSamples
	 * @param negtestSamples
	 * @param trainSet
	 * @param k
	 */
	private static double[] runTest(ArrayList<String> postestSamples, ArrayList<Signature> negtestSamples, ArrayList<String> trainingSamples, double k) {
		 ArrayList<Signature> posTest = readInSpecificValidSignatures(postestSamples);
		 ArrayList<Signature> train = readInSpecificValidSignatures(trainingSamples);
		 ArrayList<Signature> negTest = negtestSamples;
		 
		 ImprovedTrainingSet its = new ImprovedTrainingSet(weights);
		 for ( Signature p : train)
			 its.addSignature(p);
		 
		 its.makeTemplates();
		 ImprovedDTWClassifier classifier = new ImprovedDTWClassifier(its);
		 
			int FRR = 0;
			int FAR = 0;
			int posTestNumber = 0;
			int negTestNumber = 0;
			
			int tp = 0;
			int tn = 0;
			int fp = 0;
			int fn = 0;
		 
		for ( Signature ts : posTest ) {
			double decision = classifier.classify(ts, k);
			if ( decision == -1 ) 
				continue;
			
			posTestNumber++;
			if ( decision == 0 ) {
				FRR++;
				fn++;
			} else if ( decision == 1 ) {
				tp++;
			}
		}
		for ( Signature ts : negTest ) {
			negTestNumber++;
			double decision = classifier.classify(ts, k);
			if ( decision == 1 ) {
				FAR++;
				fp++;
			} else {
				tn++;
			}
		}
		
		double frr = (double)fn / (fn+tp);
		double far = (double)fp / (fp+tn);
		
		//System.out.println(frr + " " + far);
		
		globalfrr += frr;
		globalfar += far;
		
		testnum++;
		double []o ={frr,far};  
		return o;
	}
	
	private static double globalfrr = 0.0;
	private static double globalfar = 0.0;
	private static double testnum = 0.0;
	
	public static void main(String args[]) {
		
		
		if ( args.length == 0 ) 
			baseDir = "..";
		else
			baseDir = args[0];
		
		weights = new double[3];
		boolean istest = true;
		if ( istest ){
			
			weights[0] = weights[1] = 1.0;
			weights[2] = 2.5;

			
			for ( int i = 1 ; i <= 16 ; i++ ) {
				double userglobalfrr = 0;
				double userglobalfar = 0;
				String userId = "0" + i;
				if ( i < 10 )
					userId = "0" + userId;
				
				ArrayList<ArrayList<ArrayList<String>>> testAndTrains = readInTestSet(userId, 8);
				if ( testAndTrains == null ) 
					continue;
				
				int testNum = testAndTrains.size();
				
				for ( int k = 0 ; k < testAndTrains.size() ; k++ ) {
					ArrayList<ArrayList<String>> testAndTrain = testAndTrains.get(k);
					ArrayList<ArrayList<String>> testSetsForUsers = new ArrayList<ArrayList<String>>(17);
					ArrayList<ArrayList<String>> trainSetsForUsers = new ArrayList<ArrayList<String>>(17);
					testSetsForUsers.add(null);		//a 0. indexre teszünk egy üres halmazt
					trainSetsForUsers.add(null);	//a 0. indexre teszünk egy üres halmazt			
					
					if ( testAndTrain == null ) {
						testSetsForUsers.add(null);
						trainSetsForUsers.add(null);
						continue;
					}
					testSetsForUsers.add(testAndTrain.get(0));
					trainSetsForUsers.add(testAndTrain.get(1));
					ArrayList<Signature> forgery = readUserForgedSignatures(userId);
					
					double []ret = runTest(testAndTrain.get(0), forgery, testAndTrain.get(1), 4.4);
					userglobalfar += ret[1];
					userglobalfrr += ret[0];
				}
				System.out.println(userId + " global: " + (userglobalfrr/testNum) + " " + (userglobalfar/testNum));
			}
			
			System.out.println("Átlag az összes userre: " + (globalfrr/testnum) + " " + (globalfar/testnum));
			
			return;
		}
		
		double k = 2.0;	//a k paraméter kezdő értéke
		double w = 0.0; //a w súly értéke alapból
		double kStep = 0.1;
		double lastK = k;
		double wStep = 2.5;
		double startK = 4.1;
		int cycle = 0;
		
		String testTitle = "";
		String fileName = "";
		String dirName = "./results-long/sigmoid_wo_test/";
		
		while (true) {
			//if ( cycle > 15 ) {
			//	System.out.println("Vége a 15 ciklusnak.");
			//	break;
			//}
			
			if ( w > 2.5 ) {
				System.out.println("Elértük a max. súlyt");
				break;
			}
				
			w += wStep;	//léptetjük a w-t
			w = (Math.round(w*100)/100.0);	//hogy ne legyen nagyon sok tizedesjegy
			cycle++;
			
			
			weights[0] = weights[1] = 1.0;
			weights[2] = w;
			
			fileName = "_w_"+w;
			testTitle = "Elso_derivalt_zsuly_"+w;
						
			k = startK;
			
			System.out.println("Súlyok: [" + weights[0] + "," + weights[1] + "," + weights[2] + "]");
			
			ArrayList<ArrayList<String>> testSetsForUsers = new ArrayList<ArrayList<String>>(17);
			ArrayList<ArrayList<String>> trainSetsForUsers = new ArrayList<ArrayList<String>>(17);
			testSetsForUsers.add(null);		//a 0. indexre teszünk egy üres halmazt
			trainSetsForUsers.add(null);	//a 0. indexre teszünk egy üres halmazt

			for ( int i = 1 ; i <= 16 ; i++ ) {
				String userId = "0" + i;
				if ( i < 10 )
					userId = "0" + userId;
				
				ArrayList<ArrayList<ArrayList<String>>> testAndTrains = readInTestSet(userId, 8);
				ArrayList<ArrayList<String>> testAndTrain = testAndTrains.get(0); 
				if ( testAndTrain == null ) {
					testSetsForUsers.add(null);
					trainSetsForUsers.add(null);
					continue;
				}
				testSetsForUsers.add(testAndTrain.get(0));
				trainSetsForUsers.add(testAndTrain.get(1));
			}
			
			try {
				PrintWriter pw1 = new PrintWriter(new FileOutputStream(dirName+"/eer"+fileName+".txt"));
				PrintWriter pw2 = new PrintWriter(new FileOutputStream(dirName+"/roc"+fileName+".txt"));
		
				pw1.println(testTitle);
				pw2.println(testTitle);
				
				while (true) {
					int FRR = 0;
					int FAR = 0;
					int posTestNumber = 0;
					int negTestNumber = 0;
					
					int tp = 0;
					int tn = 0;
					int fp = 0;
					int fn = 0;
					
					k -= kStep;	//léptetjük a k-t
					k = (Math.round(k*100)/100.0);	//hogy ne legyen nagyon sok tizedesjegy
					
					if ( k < 1.0 )
						break;

					System.out.println("k = " + k);
					
					//vizsgáljuk meg az összes aláírót
					for ( int i = 1 ; i <= 16 ; i++ ) {
					
						
						String userId = "0" + i;
						if ( i < 10 )
							userId = "0" + userId;
			
						if ( trainSetsForUsers.get(i) == null )	//nincs ilyen aláírónk 
							continue;
						
						int numberOfSignatures = trainSetsForUsers.get(i).size();	//egyébként lekérjük a darabszámot
						
			
						int numberOfTrainingSet = numberOfSignatures / testSetRate;
						
						/*System.out.println(testSetRate + " részre osztjuk a " + (i+1) + ". aláíró valós aláírások halmazát, tehát egy halmazba a " + 
										numberOfSignatures + " db. aláírásbó " + numberOfTrainingSet + " db. kerül");*/
						
						ArrayList<Signature> forgery = readUserForgedSignatures(userId);	//az aktuálisan vizsgált személy hamisításai
						
						//l jelöli, hogy most épp hanyadik validálást hajtjuk végre
						for ( int l = 0 ; l < testSetRate ; l++ ) {
						
							//System.out.println((k+1) + " tanítás.");
							ArrayList<Signature> usersSig = readInSpecificValidSignatures((trainSetsForUsers.get(i)));
							
							
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
								
								posTestNumber++;
								if ( decision == 0 ) {
									FRR++;
									fn++;
								} else if ( decision == 1 ) {
									tp++;
								}
							}
							for ( Signature ts : forgery ) {
								negTestNumber++;
								double decision = classifier.classify(ts, k);
								if ( decision == 1 ) {
									FAR++;
									fp++;
								} else {
									tn++;
								}
							}
						}
					}
					
					//double frr = (FRR*100/(double)posTestNumber);
					//double far = (FAR*100/(double)negTestNumber);
					double frr = (double)fn / (fn+tp);
					double far = (double)fp / (fp+tn);
					//System.out.println(k + " " + (FRR*100/(double)posTestNumber) + " " + (FAR*100/(double)negTestNumber));
					
					pw1.println(k + " " + frr + " " + far);
					pw2.println(k + " " + ((double)tp/(tp+fn)) + " " + ((double)fp/(tn+fp)));
		
					pw1.flush();
					pw2.flush();
					
					//if ( frr == far )
					//	break;

					//if ( frr < far ) {

					//} else {
						//step /= 2;
						//k += step;
					//}
					//System.out.println("Összesen " + posTestNumber + " helyes esetből " + FRR + " esetben hibázott. Ez " + (FRR*100/(double)posTestNumber) + "% eredmény.");
					//System.out.println("Összesen " + negTestNumber + " hamis esetből " + FAR + " esetben hibázott. Ez " + (FAR*100/(double)negTestNumber) + "% eredmény.");
				
				}
				pw1.close();
				pw2.close();
			} catch (IOException e ){
				e.printStackTrace();
			}
		}
	}
	
}
