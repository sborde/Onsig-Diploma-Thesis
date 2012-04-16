
import java.awt.font.NumericShaper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import evaluate.FastDTWClassifier;

import signature.*;
import training.ImprovedTrainingSet;

public class TestImprovedDtw {

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
            if ( Pattern.matches(userId+"_[0-9]^*[0-9]*.HWR", genuinefiles[i]) )
            	userSigs.add(readInSignature("genuine", genuinefiles[i]));
            
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
	
	private static void normalizeCoordWeights() {
		double sum = 0.0;
		for ( int i = 0 ; i < weights.length ; i++ )
			sum += weights[i];
		
		for ( int i = 0 ; i < weights.length ; i++ )
			weights[i] /= sum;
		
	}
	
	public static void main(String args[]) {
		
		weights = new double[3];
		
		if ( args.length == 0 ) 
			baseDir = "..";
		else
			baseDir = args[0];
		
		double k = 2.0;	//a k paraméter kezdő értéke
		double w = -2.5; //a w súly értéke alapból
		double kStep = 0.1;
		double lastK = k;
		double wStep = 2.5;
		double startK = 3.1;
		int cycle = 0;
		
		String testTitle = "";
		String fileName = "";
		String dirName = "./results-long";
		
		while (true) {
			//if ( cycle > 15 ) {
			//	System.out.println("Vége a 15 ciklusnak.");
			//	break;
			//}
			
			if ( w > 10.0 ) {
				System.out.println("Elértük a max. súlyt");
				break;
			}
				
			w += wStep;	//léptetjük a w-t
			w = (Math.round(w*100)/100.0);	//hogy ne legyen nagyon sok tizedesjegy
			cycle++;
			
			//weights[0] = weights[1] = w;
			//weights[2] = (3-2*w);
			
			weights[0] = weights[1] = 1.0;
			weights[2] = w;
			
			fileName = "_w_"+w;
			testTitle = "Elso_derivalt_zsuly_"+w;
			
			//normalizeCoordWeights();
			
			k = startK;
			
			System.out.println("Súlyok: [" + weights[0] + "," + weights[1] + "," + weights[2] + "]");
			
			try {
				PrintWriter pw1 = new PrintWriter(new FileOutputStream(dirName+"/eer"+fileName+".txt"));
				PrintWriter pw2 = new PrintWriter(new FileOutputStream(dirName+"/roc"+fileName+".txt"));
				/*
				PrintWriter dpw = new PrintWriter(new FileOutputStream("./distances.txt", true));
				dpw.println("[" + weights[0] + "," + weights[1] + "," + weights[2] + "]");
				dpw.flush();
				dpw.close();
				*/
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
					
					if ( k < 0.0 )
						break;

					System.out.println("k = " + k);
					
					//vizsgáljuk meg az összes aláírót
					for ( int i = 1 ; i <= 16 ; i++ ) {
					//int i = 1;
						
						String userId = "0" + i;
						if ( i < 10 )
							userId = "0" + userId;
			
						int numberOfSignatures = getSignaturesNumber(userId);
						if ( numberOfSignatures == 0 ) 
							continue;
						
			
						int numberOfTrainingSet = numberOfSignatures / testSetRate;
						
						/*System.out.println(testSetRate + " részre osztjuk a " + (i+1) + ". aláíró valós aláírások halmazát, tehát egy halmazba a " + 
										numberOfSignatures + " db. aláírásbó " + numberOfTrainingSet + " db. kerül");*/
						
						ArrayList<Signature> forgery = readUserForgedSignatures(userId);	//az aktuálisan vizsgált személy hamisításai
						
						//l jelöli, hogy most épp hanyadik tesztelést hajtjuk végre
						for ( int l = 0 ; l < testSetRate ; l++ ) {
						
							//System.out.println((k+1) + " tanítás.");
							ArrayList<Signature> usersSig = readUserValidSignatures(userId);
							
							
							ImprovedTrainingSet train = new ImprovedTrainingSet(weights);	//teszthalmaz létrehozása
							
							ArrayList<Signature> test = new ArrayList<Signature>();	//teszt aláírások gyűjtőhelye
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
														
							FastDTWClassifier classifier = new FastDTWClassifier(train);
							
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
