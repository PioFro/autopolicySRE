package org.rnn;

import java.util.ArrayList;
/**
 * Class handling the proactive approach, based on the curve prediction for SRE
 * @author Piotr Frohlich
 * @version 1.0.0
 */

public class CurveManager
{
    /**
     * Curve histogram
     */
    private ArrayList<Double> histogram = new ArrayList<>();

    /**
     * Gets as a parameter load on forwarder in [Mb/s] and returns the expected
     * energy cost derived from the power/load characteristic.
     * @param load Load value of the given forwarder
     * @return Goal function ready value of the expected energy usage.
     */
    public static double getGoalParticleFromLoad(float load)
    {
        // Instant value given the the interpolated curve.
        double x = 0.0;
        // The exact value of the expected energy consumption
        double c = 0.0;
        if(load >= 0 && load <= 250)
        {
            x = load;
            c = 6+ (8*((0.001 * x * x)/62.001));
        }
        else if(load > 250 && load<= 400)
        {
            x = load - 250;
            c = 14 - 6*((0.001 *x*x)/22.201);
        }
        else if(load > 400 && load <= 550)
        {
            x = load - 400 + 2;
            c = 9+4*(Math.log(x/2)/4.324132656254979);
        }
        else if(load>550)
        {
            c = 13.0;
        }
        return load/(c*c);
    }

    /**
     * Basic constructor which instantiates the curve with the threshold set to
     * the given parameter level.
     * @param maxThroughputPercentage max load before delay skyrockets
     */
    public CurveManager(int maxThroughputPercentage)
    {
        double stepQ= 1.1, stepP = 0.02;
        for (int i = 0; i <100; i++)
        {
            if(i < (maxThroughputPercentage-10))
            {
                histogram.add(1+stepP);
                stepP+=0.02;
            }
            else
            {
                histogram.add(stepQ*(1+stepP));
                stepQ*=1.15;
            }
        }
    }
    public CurveManager()
    {
        for (int i = 0; i < 1024; i++)
        {
            if(i<200)
            {
                histogram.add(20+(double)i/200);
            }
            else if(i<400)
            {
                histogram.add(25+(double)i/200);
            }
            else
            {
                histogram.add(20+(double)i/200);
            }
        }
    }
    public CurveManager(ArrayList<Double> h)
    {
        histogram = h;
    }

    /**
     * Getter for predicted value of the delay after adding another portion of the
     * load to the device.
     * @param percentage Percentage of fullness of the forwarder.
     * @return Predicted delay value for given fullness of the forwarder.
     */
    public double getPredictedDelayValue(int percentage)
    {
        percentage-=1;
        if(percentage<histogram.size()&&percentage>=0)
        {
            return histogram.get(percentage);
        }
        return histogram.get(histogram.size()-1);
    }

}
