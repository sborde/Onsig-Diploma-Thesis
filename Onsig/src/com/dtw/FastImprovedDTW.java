/*
 * FastDTW.java   Jul 14, 2004
 *
 * Copyright (c) 2004 Stan Salvador
 * stansalvador@hotmail.com
 */

package com.dtw;

import com.timeseries.TimeSeries;
import com.timeseries.PAA;


public class FastImprovedDTW
{
   // CONSTANTS
   final static int DEFAULT_SEARCH_RADIUS = 1;


   public static double getWarpDistBetween(TimeSeries tsI, TimeSeries tsJ, double[] weights)
   {
      return fastDTW(tsI, tsJ, DEFAULT_SEARCH_RADIUS, weights, null).getDistance();
   }
   
   public static double getWarpDistBetween(TimeSeries tsI, TimeSeries tsJ, double[] coordWeights, double[] pointWeights)
   {
      return fastDTW(tsI, tsJ, DEFAULT_SEARCH_RADIUS, coordWeights, pointWeights).getDistance();
   }


   public static double getWarpDistBetween(TimeSeries tsI, TimeSeries tsJ, int searchRadius, double[] weights)
   {
      return fastDTW(tsI, tsJ, searchRadius, weights, null).getDistance();
   }


   public static WarpPath getWarpPathBetween(TimeSeries tsI, TimeSeries tsJ, double[] weights)
   {
      return fastDTW(tsI, tsJ, DEFAULT_SEARCH_RADIUS, weights, null).getPath();
   }


   public static WarpPath getWarpPathBetween(TimeSeries tsI, TimeSeries tsJ, int searchRadius, double[] weights)
   {
      return fastDTW(tsI, tsJ, searchRadius, weights, null).getPath();
   }


   public static TimeWarpInfo getWarpInfoBetween(TimeSeries tsI, TimeSeries tsJ, int searchRadius, double[] weights)
   {
      return fastDTW(tsI, tsJ, searchRadius, weights, null);
   }


   private static TimeWarpInfo fastDTW(TimeSeries tsI, TimeSeries tsJ, int searchRadius, double[] weights, double[] pointWeights)
   {	   
      if (searchRadius < 0)
         searchRadius = 0;

      final int minTSsize = searchRadius+2;

      if ( (tsI.size() <= minTSsize) || (tsJ.size()<=minTSsize) )
      {
         // Perform full Dynamic Time Warping.
         return ImprovedDTW.getWarpInfoBetween(tsI, tsJ, weights);
      }
      else
      {
         final double resolutionFactor = 2.0;

         final PAA shrunkI = new PAA(tsI, (int)(tsI.size()/resolutionFactor));
         final PAA shrunkJ = new PAA(tsJ, (int)(tsJ.size()/resolutionFactor));

          // Determine the search window that constrains the area of the cost matrix that will be evaluated based on
          //    the warp path found at the previous resolution (smaller time series).
          final SearchWindow window = new ExpandedResWindow(tsI, tsJ, shrunkI, shrunkJ,
                                                            FastImprovedDTW.getWarpPathBetween(shrunkI, shrunkJ, searchRadius, weights),
                                                            searchRadius);

         // Find the optimal warp path through this search window constraint.
         return ImprovedDTW.getWarpInfoBetween(tsI, tsJ, window, weights);
      }  // end if
   }  // end recFastDTW(...)

}  // end class fastDTW
