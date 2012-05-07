package signature;

import com.timeseries.TimeSeries;
import com.timeseries.TimeSeriesPoint;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Beolvassa egy aláírás adatpontjait fájlból,
 * de csak meghatározott oszlopokat vesz figyelembe.
 * @author sborde
 */
public class Signature {

	protected String fileName;
	
    /**
     * Egy aláírás szegmensenként tárolva. A felemelt tollú
     * és író szakaszokat nem különböztetjük meg, úgyis váltakoznak.
     */
    protected ArrayList<com.timeseries.TimeSeries> segments;
    
    /**
     * Az aláírások eredeti állapotban, későbbi használatra megőrizzük.
     */
    protected ArrayList<com.timeseries.TimeSeries> originalSegments;

	/**
     * A teljes aláírás nem szegmentálva. A könnyebb kezelhetőség miatt.
     */
    protected com.timeseries.TimeSeries wholeSignature;

    /**
     * Egy adott szegmens írási szakasz-e.
     */
    protected ArrayList<Boolean> segmentPenDown;

    /**
     * Összes írással töltött idő.
     */
    protected int totalPenDownTime;

    /**
     * Összes felemelt tollú idő.
     */
    protected int totalPenUpTime;

    
    
    public String getFileName() {
		return fileName;
	}

	/**
     * Visszaadja azt az időt, amíg a toll a felületen volt.
     * @return letett tollú idő
     */
    public int getTotalPenDownTime() {
    	totalPenDownTime = 0;
    	for ( int i = 0 ; i < segments.size() ; i++ ) {	//sorra vesszük a szegmenseket
    		if ( !this.segmentPenDown.get(i) )	//csak a lerakott tollú szakasz érdekel most
    			continue;
    		
    		totalPenDownTime += getSegmentTime(i);
    	}
        return totalPenDownTime;
    }
    
    /**
     * Lekéri az i. szegmens idejét.
     * @param i szegmens indexe
     * @return szegmens ideje
     */
    public int getSegmentTime(int i) {
    	return this.segments.get(i).size();
    }

    /**
     * Visszaadja azt az időt, amíg a toll fel volt emelve.
     * @return felemelt idő
     */
    public int getTotalPenUpTime() {
    	totalPenUpTime = 0;
    	for ( int i = 0 ; i < segments.size() ; i++ ) {	//sorra vesszük a szegmenseket
    		if ( this.segmentPenDown.get(i) )	//csak a felemelt tollú szakasz érdekel most
    			continue;
    		
    		totalPenUpTime += getSegmentTime(i);
    	}    	
        return totalPenUpTime;
    }

    /**
     * Kiszámítja és visszaadja a teljes aláírás időt.
     * @return teljes idő
     */
    public int getTotalTime() {
        return getTotalPenDownTime() + getTotalPenUpTime();
    }

    /**
     * Visszaad egy bizonyos aláírás szegmenst.
     * @param i a szegmens indexe
     * @return a kért szegmens
     */
    public com.timeseries.TimeSeries getSegment(int i) {
        return segments.get(i);
    }

    /**
     * Visszaadja az egyes szakaszok toll állapotát (író vagy nem író) az 
     * egyes szegmensekre.
     * @return állapotok listája
     */
    public ArrayList<Boolean> getSegmentPenDown() {
		return segmentPenDown;
	}
    
    /**
     * Visszaadja a teljes aláírást, szegmensekre bontva.
     * @return szegmensek listája
     */
    public ArrayList<com.timeseries.TimeSeries> getSegments() {
		return segments;
	}
    
    /**
     * Visszaadja az aláírást teljes egészében, szegmentálatlanul.
     * @return az aláírás teljes idősora
     */
    public TimeSeries getWholeSignature() {
        return wholeSignature;
    }    
   
    /**
     * Hány szakaszból áll az aláírás.
     * @return szakaszok száma
     */
    public int numberOfSegments() {
        return segments.size();
    }
    
    /**
     * Visszaadja, hogy egy adott szegmens írási vagy 
     * felemelt tollú-e.
     * @param i vizsgált szegmens indexe
     * @return igaz, ha írási szegmens volt
     */
    public boolean isPendownSegment(int i) {
    	return segmentPenDown.get(i);
    }
    
    /**
     * Paraméterben kapott idősort újramintavételezi.
     * @param segment újramintavételezendő idősor
     * @param newLength új hossz
     * @return az új idősor
     */
    public TimeSeries resampleGivenSegment(TimeSeries segment, int newLength) {

    	/* Ha megegyezik a két szakasz hossza, akkor nem kell újramintavételezni. */
    	//if ( newLength == this.segments.get(index).numOfPts() ) 
    		//return;
    	
        double sampleRate = (segment.numOfPts()-1) / (double)(newLength-1);    //mintavételezési ráta, a 0. pont mindig a helyén marad, így azt nem számoljuk 
        double lambda, egyMinusLambda;  //lambda és 1-lambda
                
        com.timeseries.TimeSeries segmentToResample = segment; //az újramintavételezni kívánt szegmens
        int dimension = segmentToResample.numOfDimensions();    //egy pontban hány érték van
        com.timeseries.TimeSeries segmentResampled = new com.timeseries.TimeSeries(dimension);  //ez lesz az újramintavételezett szegmens

        if (segmentToResample.size() == 0) {
        	return segment;
        }
        
        int i = 0;  //ez halad az új tömbön
        double j = 0.0; //a régi sorozat ezen elemét kell illeszteni az i. új helyre. Ha ez nem rácspont, akkor lin. interpoláció kell
       
        while ( i < newLength ) {        	
            lambda = j - Math.floor(j);             //lambda az előző ponttól vett távolság
            egyMinusLambda = 1 - lambda;      //1-lambda a következő ponttól való távolság

            double []resampledPointCoords = new double[dimension];  //az új adatok az adott időpontban időpontban
            
            double []oldSegmentx0 = segmentToResample.getMeasurementVector((int)Math.floor(j));  //bal végpont
            double []oldSegmentx1;
            if ( Math.ceil(j) >= segmentToResample.numOfPts()) {
            	oldSegmentx1 = oldSegmentx0;
            } else {
            	oldSegmentx1 = segmentToResample.getMeasurementVector((int)Math.ceil(j));   //jobb végpont (rácspont esetén a kettő megegyezik
            }

            /* Az időpillanat minden koordinátáját egyesével újramintavételezem */
            for ( int k = 0 ; k < dimension ; k++ ) {
                resampledPointCoords[k] = oldSegmentx0[k]*egyMinusLambda + oldSegmentx1[k]*lambda;
            }
            
            com.timeseries.TimeSeriesPoint resampledTimePoint = new TimeSeriesPoint(resampledPointCoords);  //az i. új időpont
            
            segmentResampled.addLast(i, resampledTimePoint);
            
            i++;
            j += sampleRate;
        }
        return segmentResampled;
    }
    
    /**
     * Egy szegmens újramintavételezését végzi. A köztes pontok
     * számításához a két végpontot használja fel, és a távolságukkal
     * arányosan számolja őket.
     * @param index szegmens sorszáma
     * @param newLength új szegmens hossza
     */
    public void resampleSegment(int index, int newLength) {
        this.segments.set(index, resampleGivenSegment(this.segments.get(index), newLength)); //cserélem a szegmenst
    }
    
    /**
     * A teljes aláírást újramintavételezi.
     * @param newLength az aláírás új hossza
     */
    public void resampleWholeSignature(int newLength) {
    	this.wholeSignature = resampleGivenSegment(this.wholeSignature, newLength);
    }

    /**
     * Alapértelmezett konstruktor.
     */
    public Signature() {
    	this.segmentPenDown = new ArrayList<Boolean>();
    	this.segments = new ArrayList<com.timeseries.TimeSeries>();
    	this.originalSegments = new ArrayList<com.timeseries.TimeSeries>();
    	this.totalPenDownTime = 0;
    	this.totalPenUpTime = 0;
    }

    /**
     * Konstruktor, mely egy másik aláírást lemásolva hozza létre
     * az újat. A leszármazott template signature konstruktora miatt
     * fog kelleni.
     * @param source másolandó aláírás
     */
    public Signature(Signature source) {
    	this.wholeSignature = source.getWholeSignature();
    	this.segments = source.getSegments();
    	this.segmentPenDown = source.getSegmentPenDown();
    	this.totalPenDownTime = source.getTotalPenDownTime();
    	this.totalPenUpTime = source.getTotalPenUpTime();
    }
    private double[] coordWeights;
	/**
     * Létrehozza az aláírást a createTimeSeries metódus
     * meghívásával. Egy tömbben lehet megadni, hogy hanyadik oszlopok
     * kerüljenek beolvasásra. Ezzel dinamikusan kezelhetjük, hogy mely
     * mezőket vegyük figyelembe, az adatbázis megváltoztatása nélkül.
     * @param file beolvasandó fájl neve
     * @param numOfPoints beolvasandó pontok száma
     * @param cols beolvasandó oszlopok
     */
    public Signature(String file, int numOfPoints, int []cols, double[] weights) {
        try {
        	this.fileName = file;
        	this.coordWeights = weights;
            createTimeSeries(file, numOfPoints, cols);
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Nincs ilyen file: " + file);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Visszaállítja az aláírást az eredeti állapotába.
     */
    public void reset() {
    	this.segments = this.originalSegments;
    }
    
    /**
     * A konstruktor hívja meg. Egy adott fájlból beolvassa az adatokat, és
     * létrehozza az idősor objektumokat.
     * @param file beolvasandó fájl neve
     * @param numOfPoints beolvasandó adatok száma
     * @param cols a beolvasandó adatok mely oszlopokban vannak, így nem szükséges egymás utáni oszlopokat olvasni
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void createTimeSeries(String file, int numOfPoints, int []cols) throws FileNotFoundException, IOException {
        BufferedReader filein = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;

        int totalTime = 0;
        int lineNum = -1;
        boolean penDown = true;
        int currentSegmentsLength = 0;

        wholeSignature = new com.timeseries.TimeSeries(numOfPoints);
        segments = new ArrayList<com.timeseries.TimeSeries>();
        originalSegments = new ArrayList<com.timeseries.TimeSeries>();
        segmentPenDown = new ArrayList<Boolean>();

        com.timeseries.TimeSeries segment = new com.timeseries.TimeSeries(numOfPoints); //egy aláírásszegmens idősora
        com.timeseries.TimeSeries segmentOrigi = new com.timeseries.TimeSeries(numOfPoints); //egy aláírásszegmens idősora
        segments.add(segment);  //hozzáadjuk a szegmenseket
        originalSegments.add(segmentOrigi);  //hozzáadjuk a szegmenseket
        
        while ((line = filein.readLine())!=null) {  //soronként olvasunk

            lineNum++;
            int currCol = 0;    //első oszloppal kezdjük
            int whichColNeed = 0;   //hanyadik sor kell
            double[] val = new double[numOfPoints];    //a sorból beolvasott adatok

            StringTokenizer st = new StringTokenizer(line," "); //felbontás szóközök mentén
                       
            while ( st.hasMoreTokens() ){

                String token = st.nextToken();  //vesszük a sor következő szavát
                
                if ( currCol == 2 ) {   //ha a z koordinátát olvassuk

                    if ( Double.parseDouble(token) == 0.0 ) { //és az 0
                    	if ( lineNum == 0 ) 	//ha az első sorban voltunk, akkor felemelt tollal kezdtünk
                    		penDown = false;
                    	
                        if ( penDown ) { //és a toll lent volt, akkor most felemeltük
                            segmentPenDown.add(penDown);
                            penDown = false;    //akkor a toll a levegőben van
                            
                            segment = new com.timeseries.TimeSeries(numOfPoints);
                            segmentOrigi = new com.timeseries.TimeSeries(numOfPoints);
                            
                            segments.add(segment);
                            originalSegments.add(segmentOrigi);
                            
                            currentSegmentsLength = 0;
                        }
                    } else {    //ha nem 0-t olvasunk
                        if ( !penDown ) {   //és eddig fent volt a toll
                            segmentPenDown.add(penDown);
                            penDown = true;    //akkor a toll a levegőben van
                            segment = new com.timeseries.TimeSeries(numOfPoints);
                            segmentOrigi = new com.timeseries.TimeSeries(numOfPoints);
                            
                            segments.add(segment);
                            originalSegments.add(segmentOrigi);
                            
                            currentSegmentsLength = 0;
                        }
                    }
                }
                                
                if ( currCol > cols[cols.length-1] ){    //ha ezeket már nem kell beolvasni, akkor ne menjünk tovább
                    break;
                } else if ( currCol < cols[0] ) {  //ha ezt még nem kell beolvasni, ugorjunk
                    currCol++;
                    continue;
                } else if ( currCol == cols[whichColNeed] ) { //ezt be kell olvasni
                    val[whichColNeed] = Double.parseDouble(token)*coordWeights[whichColNeed];
                    currCol++;
                    whichColNeed++;
                }

            }

            com.timeseries.TimeSeriesPoint point = new TimeSeriesPoint(val);    //új időpont létrehozása a beolvasott értékekkel
            
            segment.addLast(currentSegmentsLength, point);      //időpont hozzáadása a szegmenshez
            segmentOrigi.addLast(currentSegmentsLength, point);	//és az eredetibe is másoljuk
            
            wholeSignature.addLast(totalTime, point);           //időpont hozzáadása a teljes aláíráshoz
            
            currentSegmentsLength++;    //szegmensben lévő időpontok számának növelése
            totalTime++;
        }//end of while
        segmentPenDown.add(penDown);

        for ( int i = 0 ; i < segmentPenDown.size() ; i++ ){
            if ( segmentPenDown.get(i) )
                this.totalPenDownTime += segments.get(i).numOfPts();
            else
                this.totalPenUpTime += segments.get(i).numOfPts();
        }

        /*System.out.println("Number of segments: " + segments.size());
        System.out.println("Legth of segments': ");
        for ( int i = 0 ; i < segments.size() ; i++ ) {
            System.out.println((i+1) + ". szegmens: " + segments.get(i).numOfPts() + " " + segmentPenUp.get(i));
        }*/
        //System.out.println(wholeSignature.numOfPts());
        filein.close();
    }

}
