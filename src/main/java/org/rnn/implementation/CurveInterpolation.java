package org.rnn.implementation;

import java.util.ArrayList;


public class CurveInterpolation
{
    double maxX;
    double minX;
    double step;
    public ArrayList<Double> iX = new ArrayList<Double>();
    public ArrayList<Double> iY = new ArrayList<Double>();

    public CurveInterpolation(double min, double max, double step,ArrayList<Double> x, ArrayList<Double> y) throws Exception
    {
        maxX =max;
        minX = min;
        this.step =step;
        if(x.size()!=y.size())
            throw new InvalidSizeException("x is not equal in size y");
        Double prev = x.get(0)-1.0;
        for (Double d:x
             )
        {
            if(d<prev)
                throw new InvalidSizeException("X does not represent a function");
            prev = d;
        }
        int iter = 0;
        for(double i = minX;i<=maxX;i+=step)
        {
            //Iterate further
            if(i>x.get(iter)&&iter!=x.size()-1)
            {
                iX.add(x.get(iter));
                iY.add(y.get(iter));
                iter+=1;
            }
            else
            {
                iX.add(i);
                iY.add(y.get(iter));
            }
        }
    }
}
