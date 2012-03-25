
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
	public static int []cols = {3,4,5,9,10,11};
	
	/**
	 * Az egyes koordináták súlyai.
	 */
	public static double []weights = {1,1,1,1,1,1};
	
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
		
		return new Signature("../data-deriv/" + type + "/" + name, cols.length, cols);
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
	
	public static void main(String args[]) {
		
		if ( args.length == 0 ) 
			baseDir = "..";
		else
			baseDir = args[0];
		
		double k = 2;	//a k paraméter kezdő értéke
		double lastK = k;
		double step = 0.1;
		while (true) {
			int FRR = 0;
			int FAR = 0;
			int posTestNumber = 0;
			int negTestNumber = 0;
			
			//vizsgáljuk meg az összes aláírót
			for ( int i = 1 ; i <= 16 ; i++ ) {
				
				String userId = "0" + i;
				if ( i < 10 )
					userId = "0" + userId;
	
				int numberOfSignatures = getSignaturesNumber(userId);
				if ( numberOfSignatures == 0 ) {
					//System.out.println("Nincs " + userId + " azonosítójú aláírónk, tovább...");
					continue;
				}
	
				int numberOfTrainingSet = numberOfSignatures / testSetRate;
				
				/*System.out.println(testSetRate + " részre osztjuk a " + (i+1) + ". aláíró valós aláírások halmazát, tehát egy halmazba a " + 
								numberOfSignatures + " db. aláírásbó " + numberOfTrainingSet + " db. kerül");*/
				
				ArrayList<Signature> forgery = readUserForgedSignatures(userId);	//az aktuálisan vizsgált személy hamisításai
				
				//k jelöli, hogy most épp hanyadik tesztelést hajtjuk végre
				for ( int l = 0 ; l < testSetRate ; l++ ) {
					//System.out.println((k+1) + " tanítás.");
					ArrayList<Signature> usersSig = readUserValidSignatures(userId);
					
					ImprovedTrainingSet train = new ImprovedTrainingSet(weights);	//teszthalmaz létrehozása
					
					ArrayList<Signature> test = new ArrayList<Signature>();	//teszt aláírások gyűjtőhelye
					for ( int j = 0 ; j < usersSig.size() ; j++ ) {
						Signature s = usersSig.get(j);
						s.reset();	//visszaállítjuk mindig az eredeti állapotra az aláírást
						if ( j < l*numberOfTrainingSet || j >= (l+1)*numberOfTrainingSet ) {
							//System.out.println(s.getFileName());
							train.addSignature(s);
						} else {
							test.add(s);
						}
					}
					
					train.makeTemplates();
					FastDTWClassifier classifier = new FastDTWClassifier(train);
					
					for ( Signature ts : test ) {
						posTestNumber++;
						boolean decision = classifier.classify(ts, k);
						//System.out.println(decision);
						if ( !decision )
							FRR++;
					}
					for ( Signature ts : forgery ) {
						negTestNumber++;
						boolean decision = classifier.classify(ts, k);
						
						if ( decision )
							FAR++;
					}
				}
			}
			
			double frr = (FRR*100/(double)posTestNumber);
			double far = (FAR*100/(double)negTestNumber);
			System.out.println(k + " " + (FRR*100/(double)posTestNumber) + " " + (FAR*100/(double)negTestNumber));
			
			if ( frr == far )
				break;
			if ( k == 0 )
				break;
			
			//if ( frr < far ) {
				lastK = k;
				k -= step;
			//} else {
				//step /= 2;
				//k += step;
			//}
			//System.out.println("Összesen " + posTestNumber + " helyes esetből " + FRR + " esetben hibázott. Ez " + (FRR*100/(double)posTestNumber) + "% eredmény.");
			//System.out.println("Összesen " + negTestNumber + " hamis esetből " + FAR + " esetben hibázott. Ez " + (FAR*100/(double)negTestNumber) + "% eredmény.");
		}
	}
	
}
