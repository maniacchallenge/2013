package de.uni_bremen.comnets.maniac.util;

/**
 *
 *
 * Created by Isaac Supeene on 6/27/13.
 */
public class NumericalMethods {
    private static final double phi = (1 + Math.sqrt(5)) / 2;

    private static Integer nextLowerFibonacciNumber(Integer value) {
        if (value < 2) {
            return value;
        }
        else {
            return fibonacci((int)Math.floor(Math.log(value * Math.sqrt(5)) / Math.log(phi)));
        }
    }

    private static Integer fibonacci(Integer value) {
        return (int)((Math.pow(phi, value) - Math.pow(1 - phi, value)) / Math.sqrt(5));
    }

    public static Pair<Integer, Double> fibonacciMax(Function<Integer, Double> f, Integer lowerBound, Integer upperBound) {
        Integer nextFibonacci = nextLowerFibonacciNumber(upperBound - lowerBound);
        if (nextFibonacci == upperBound - lowerBound) {
            return fibonacciMaxInit(f, lowerBound, upperBound);
        }
        else {
            Integer lowerMidpoint = upperBound - nextFibonacci;
            Integer upperMidpoint = lowerBound + nextFibonacci;
            Double lowerMidpointVal = f.evaluate(lowerMidpoint);
            Double upperMidpointVal = f.evaluate(upperMidpoint);

            if (lowerMidpointVal > upperMidpointVal) {
                return fibonacciMaxInit(f, lowerBound, upperMidpoint);
            }
            else {
                return fibonacciMaxInit(f, lowerMidpoint, upperBound);
            }
        }
    }

    private static Pair<Integer, Double> fibonacciMaxInit(Function<Integer, Double> f, Integer lowerBound, Integer upperBound) {
        if (f == null || lowerBound == null || upperBound == null) {
            throw new IllegalArgumentException("fibonacciMax algorithm cannot proceed with null arguments.");
        }

        if (upperBound < lowerBound) {
            throw new IllegalArgumentException("Upper bound cannot be lower than lower bound.");
        }

        Integer midpoint = lowerBound + nextLowerFibonacciNumber(upperBound - lowerBound - 1);
        Double midpointVal = f.evaluate(midpoint);
        return fibonacciMaxImpl(f, lowerBound, midpoint, upperBound, midpointVal);
    }

    private static Pair<Integer, Double> fibonacciMaxImpl(Function<Integer, Double> f, Integer lowerBound, Integer midpoint,
                                                                                       Integer upperBound, Double midpointVal) {
        if (midpoint.equals(lowerBound) || midpoint.equals(upperBound)) {
            return Pair.make(midpoint, midpointVal);
        }


        Integer newX;
        if (midpoint - lowerBound > upperBound - midpoint) {
            newX = upperBound - (midpoint - lowerBound);
        }
        else {
            newX = lowerBound + (upperBound - midpoint);
        }

        Double newVal = f.evaluate(newX);

        if (newVal > midpointVal) {
            if (midpoint - lowerBound > upperBound - midpoint) {
                return fibonacciMaxImpl(f, lowerBound, newX, midpoint, newVal);
            }
            else {
                return fibonacciMaxImpl(f, midpoint, newX, upperBound, newVal);
            }
        }
        else {
            if (midpoint - lowerBound > upperBound - midpoint) {
                return fibonacciMaxImpl(f, newX, midpoint, upperBound, midpointVal);
            }
            else {
                return fibonacciMaxImpl(f, lowerBound, midpoint, newX, midpointVal);
            }
        }
    }

    public static Triple<Integer, Integer, Double> fibonacciMax2Var(final Function2Var<Integer, Integer, Double> f,
                                                                    final Integer lowerBoundX, final Integer upperBoundX,
                                                                    final Integer lowerBoundY, final Integer upperBoundY) {
        Integer realLowerBoundX, realLowerBoundY, realUpperBoundX, realUpperBoundY;

        Integer nextFibonacciX = nextLowerFibonacciNumber(upperBoundX - lowerBoundX);
        if (nextFibonacciX == upperBoundX - lowerBoundX) {
            realLowerBoundX = lowerBoundX;
            realUpperBoundX = upperBoundX;
        }
        else {
            final Integer lowerMidpointX = upperBoundX - nextFibonacciX;
            final Integer upperMidpointX = lowerBoundX + nextFibonacciX;
            Double xLowerMidpointVal = fibonacciMax(new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer y) {
                    return f.evaluate(lowerMidpointX, y);
                }
            }, lowerBoundY, upperBoundY).getSecond();
            Double xUpperMidpointVal = fibonacciMax(new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer y) {
                    return f.evaluate(upperMidpointX, y);
                }
            }, lowerBoundY, upperBoundY).getSecond();

            if (xLowerMidpointVal > xUpperMidpointVal) {
                realLowerBoundX = lowerBoundX;
                realUpperBoundX = upperMidpointX;
            }
            else {
                realLowerBoundX = lowerMidpointX;
                realUpperBoundX = upperBoundX;
            }
        }

        Integer nextFibonacciY = nextLowerFibonacciNumber(upperBoundY - lowerBoundY);
        if (nextFibonacciY == upperBoundY - lowerBoundY) {
            realLowerBoundY = lowerBoundY;
            realUpperBoundY = upperBoundY;
        }
        else {
            final Integer lowerMidpointY = upperBoundY - nextFibonacciY;
            final Integer upperMidpointY = lowerBoundY + nextFibonacciY;
            Double yLowerMidpointVal = fibonacciMax(new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer x) {
                    return f.evaluate(x, lowerMidpointY);
                }
            }, lowerBoundX, upperBoundX).getSecond();
            Double yUpperMidpointVal = fibonacciMax(new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer x) {
                    return f.evaluate(x, upperMidpointY);
                }
            }, lowerBoundX, upperBoundX).getSecond();

            if (yLowerMidpointVal > yUpperMidpointVal) {
                realLowerBoundY = lowerBoundY;
                realUpperBoundY = upperMidpointY;
            }
            else {
                realLowerBoundY = lowerMidpointY;
                realUpperBoundY = upperBoundY;
            }
        }

        return fibonacciMax2VarInit(f, realLowerBoundX, realUpperBoundX, realLowerBoundY, realUpperBoundY);
    }

    private static Triple<Integer, Integer, Double> fibonacciMax2VarInit(final Function2Var<Integer, Integer, Double> f,
                                                                    final Integer lowerBoundX, final Integer upperBoundX,
                                                                    final Integer lowerBoundY, final Integer upperBoundY) {
        if (f == null || lowerBoundX == null || upperBoundX == null || lowerBoundY == null || upperBoundY == null) {
            throw new IllegalArgumentException("fibonacciMax algorithm cannot proceed with null arguments.");
        }

        if (upperBoundX < lowerBoundX || upperBoundY < lowerBoundY) {
            throw new IllegalArgumentException("Upper bound cannot be lower than lower bound.");
        }

        final Integer midpointX = lowerBoundX + nextLowerFibonacciNumber(upperBoundX - lowerBoundX - 1);
        final Integer midpointY = lowerBoundY + nextLowerFibonacciNumber(upperBoundY - lowerBoundY - 1);

        Double xMidpointVal = fibonacciMax(new Function<Integer, Double>() {
            @Override
            public Double evaluate(Integer y) {
                return f.evaluate(midpointX, y);
            }
        }, lowerBoundY, upperBoundY).getSecond();

        Double yMidpointVal = fibonacciMax(new Function<Integer, Double>() {
            @Override
            public Double evaluate(Integer x) {
                return f.evaluate(x, midpointY);
            }
        }, lowerBoundX, upperBoundX).getSecond();

        return fibonacciMax2VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                       lowerBoundY, midpointY, upperBoundY, yMidpointVal);
    }

    private static Triple<Integer, Integer, Double> fibonacciMax2VarImpl(final Function2Var<Integer, Integer, Double> f,
                                                                        final Integer lowerBoundX, final Integer midpointX,
                                                                        final Integer upperBoundX, final Double xMidpointVal,
                                                                        final Integer lowerBoundY, final Integer midpointY,
                                                                        final Integer upperBoundY, final Double yMidpointVal) {
        if ((midpointX.equals(lowerBoundX) || midpointX.equals(upperBoundX)) &&
            (midpointY.equals(lowerBoundY) || midpointY.equals(upperBoundY)))
        {
            return Triple.make(midpointX, midpointY, xMidpointVal);
        }

        Integer deltaX = upperBoundX - lowerBoundX;
        Integer deltaY = upperBoundY - lowerBoundY;

        if (deltaX > deltaY) { // deltaX is bigger - we want to shrink deltaX
            final Integer newX;
            if (midpointX - lowerBoundX > upperBoundX - midpointX) {
                newX = upperBoundX - (midpointX - lowerBoundX);
            }
            else {
                newX = lowerBoundX + (upperBoundX - midpointX);
            }

            Double newVal = fibonacciMax(new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer y) {
                    return f.evaluate(newX, y);
                }
            }, lowerBoundY, upperBoundY).getSecond();

            if (newVal > xMidpointVal) {
                if (midpointX - lowerBoundX > upperBoundX - midpointX) {
                    return fibonacciMax2VarImpl(f, lowerBoundX, newX, midpointX, newVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal);
                }
                else {
                    return fibonacciMax2VarImpl(f, midpointX, newX, upperBoundX, newVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal);
                }
            }
            else {
                if (midpointX - lowerBoundX > upperBoundX - midpointX) {
                    return fibonacciMax2VarImpl(f, newX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal);
                }
                else {
                    return fibonacciMax2VarImpl(f, lowerBoundX, midpointX, newX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal);
                }
            }
        }
        else {
            final Integer newY;
            if (midpointY - lowerBoundY > upperBoundY - midpointY) {
                newY = upperBoundY - (midpointY - lowerBoundY);
            }
            else {
                newY = lowerBoundY + (upperBoundY - midpointY);
            }

            Double newVal = fibonacciMax(new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer x) {
                    return f.evaluate(x, newY);
                }
            }, lowerBoundX, upperBoundX).getSecond();

            if (newVal > yMidpointVal) {
                if (midpointY - lowerBoundY > upperBoundY - midpointY) {
                    return fibonacciMax2VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, newY, midpointY, newVal);
                }
                else {
                    return fibonacciMax2VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   midpointY, newY, upperBoundY, newVal);
                }
            }
            else {
                if (midpointY - lowerBoundY > upperBoundY - midpointY) {
                    return fibonacciMax2VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   newY, midpointY, upperBoundY, yMidpointVal);
                }
                else {
                    return fibonacciMax2VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, newY, yMidpointVal);
                }
            }
        }
    }

    public static Quadruple<Integer, Integer, Integer, Double> fibonacciMax3Var(final Function3Var<Integer, Integer, Integer, Double> f,
                                                                                final Integer lowerBoundX, final Integer upperBoundX,
                                                                                final Integer lowerBoundY, final Integer upperBoundY,
                                                                                final Integer lowerBoundZ, final Integer upperBoundZ) {
        Integer realLowerBoundX, realLowerBoundY, realUpperBoundX, realUpperBoundY, realLowerBoundZ, realUpperBoundZ;

        Integer nextFibonacciX = nextLowerFibonacciNumber(upperBoundX - lowerBoundX);
        if (nextFibonacciX == upperBoundX - lowerBoundX) {
            realLowerBoundX = lowerBoundX;
            realUpperBoundX = upperBoundX;
        }
        else {
            final Integer lowerMidpointX = upperBoundX - nextFibonacciX;
            final Integer upperMidpointX = lowerBoundX + nextFibonacciX;
            @SuppressWarnings("SuspiciousNameCombination") Double xLowerMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer y, Integer z) {
                    return f.evaluate(lowerMidpointX, y, z);
                }
            }, lowerBoundY, upperBoundY, lowerBoundZ, upperBoundZ).getThird();
            @SuppressWarnings("SuspiciousNameCombination") Double xUpperMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer y, Integer z) {
                    return f.evaluate(upperMidpointX, y, z);
                }
            }, lowerBoundY, upperBoundY, lowerBoundZ, upperBoundZ).getThird();

            if (xLowerMidpointVal > xUpperMidpointVal) {
                realLowerBoundX = lowerBoundX;
                realUpperBoundX = upperMidpointX;
            }
            else {
                realLowerBoundX = lowerMidpointX;
                realUpperBoundX = upperBoundX;
            }
        }

        Integer nextFibonacciY = nextLowerFibonacciNumber(upperBoundY - lowerBoundY);
        if (nextFibonacciY == upperBoundY - lowerBoundY) {
            realLowerBoundY = lowerBoundY;
            realUpperBoundY = upperBoundY;
        }
        else {
            final Integer lowerMidpointY = upperBoundY - nextFibonacciY;
            final Integer upperMidpointY = lowerBoundY + nextFibonacciY;
            Double yLowerMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer z) {
                    return f.evaluate(x, lowerMidpointY, z);
                }
            }, lowerBoundX, upperBoundX, lowerBoundZ, upperBoundZ).getThird();
            Double yUpperMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer z) {
                    return f.evaluate(x, upperMidpointY, z);
                }
            }, lowerBoundX, upperBoundX, lowerBoundZ, upperBoundZ).getThird();

            if (yLowerMidpointVal > yUpperMidpointVal) {
                realLowerBoundY = lowerBoundY;
                realUpperBoundY = upperMidpointY;
            }
            else {
                realLowerBoundY = lowerMidpointY;
                realUpperBoundY = upperBoundY;
            }
        }

        Integer nextFibonacciZ = nextLowerFibonacciNumber(upperBoundZ - lowerBoundZ);
        if (nextFibonacciZ == upperBoundZ - lowerBoundZ) {
            realLowerBoundZ = lowerBoundZ;
            realUpperBoundZ = upperBoundZ;
        }
        else {
            final Integer lowerMidpointZ = upperBoundZ - nextFibonacciZ;
            final Integer upperMidpointZ = lowerBoundZ + nextFibonacciZ;
            Double zLowerMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer y) {
                    return f.evaluate(x, y, lowerMidpointZ);
                }
            }, lowerBoundX, upperBoundX, lowerBoundY, upperBoundY).getThird();
            Double zUpperMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer y) {
                    return f.evaluate(x, y, upperMidpointZ);
                }
            }, lowerBoundX, upperBoundX, lowerBoundY, upperBoundY).getThird();

            if (zLowerMidpointVal > zUpperMidpointVal) {
                realLowerBoundZ = lowerBoundZ;
                realUpperBoundZ = upperMidpointZ;
            }
            else {
                realLowerBoundZ = lowerMidpointZ;
                realUpperBoundZ = upperBoundZ;
            }
        }

        return fibonacciMax3VarInit(f, realLowerBoundX, realUpperBoundX, realLowerBoundY, realUpperBoundY, realLowerBoundZ, realUpperBoundZ);
    }

    private static Quadruple<Integer, Integer, Integer, Double> fibonacciMax3VarInit(final Function3Var<Integer, Integer, Integer, Double> f,
                                                                                final Integer lowerBoundX, final Integer upperBoundX,
                                                                                final Integer lowerBoundY, final Integer upperBoundY,
                                                                                final Integer lowerBoundZ, final Integer upperBoundZ) {
        if (f == null || lowerBoundX == null || upperBoundX == null || lowerBoundY == null || upperBoundY == null || lowerBoundZ == null || upperBoundZ == null) {
            throw new IllegalArgumentException("fibonacciMax algorithm cannot proceed with null arguments.");
        }

        if (upperBoundX < lowerBoundX || upperBoundY < lowerBoundY || upperBoundZ < lowerBoundZ) {
            throw new IllegalArgumentException("Upper bound cannot be lower than lower bound.");
        }

        final Integer midpointX = lowerBoundX + nextLowerFibonacciNumber(upperBoundX - lowerBoundX - 1);
        final Integer midpointY = lowerBoundY + nextLowerFibonacciNumber(upperBoundY - lowerBoundY - 1);
        final Integer midpointZ = lowerBoundZ + nextLowerFibonacciNumber(upperBoundZ - lowerBoundZ - 1);

        @SuppressWarnings("SuspiciousNameCombination") Double xMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
            @Override
            public Double evaluate(Integer y, Integer z) {
                return f.evaluate(midpointX, y, z);
            }
        }, lowerBoundY, upperBoundY, lowerBoundZ, upperBoundZ).getThird();

        Double yMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
            @Override
            public Double evaluate(Integer x, Integer z) {
                return f.evaluate(x, midpointY, z);
            }
        }, lowerBoundX, upperBoundX, lowerBoundZ, upperBoundZ).getThird();

        Double zMidpointVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
            @Override
            public Double evaluate(Integer x, Integer y) {
                return f.evaluate(x, y, midpointZ);
            }
        }, lowerBoundX, upperBoundX, lowerBoundY, upperBoundY).getThird();

        return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                       lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                       lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
    }

    private static Quadruple<Integer, Integer, Integer, Double> fibonacciMax3VarImpl(final Function3Var<Integer, Integer, Integer, Double> f,
                                                                                     final Integer lowerBoundX, final Integer midpointX,
                                                                                     final Integer upperBoundX, final Double xMidpointVal,
                                                                                     final Integer lowerBoundY, final Integer midpointY,
                                                                                     final Integer upperBoundY, final Double yMidpointVal,
                                                                                     final Integer lowerBoundZ, final Integer midpointZ,
                                                                                     final Integer upperBoundZ, final Double zMidpointVal) {
        if ((midpointX.equals(lowerBoundX) || midpointX.equals(upperBoundX)) &&
            (midpointY.equals(lowerBoundY) || midpointY.equals(upperBoundY)) &&
            (midpointZ.equals(lowerBoundZ) || midpointZ.equals(upperBoundZ)))
        {
            return Quadruple.make(midpointX, midpointY, midpointZ, xMidpointVal);
        }

        Integer deltaX = upperBoundX - lowerBoundX;
        Integer deltaY = upperBoundY - lowerBoundY;
        Integer deltaZ = upperBoundZ - lowerBoundZ;

        if (deltaX > deltaY && deltaX > deltaZ) { // deltaX is the biggest - we want to shrink deltaX
            final Integer newX;
            if (midpointX - lowerBoundX > upperBoundX - midpointX) {
                newX = upperBoundX - (midpointX - lowerBoundX);
            }
            else {
                newX = lowerBoundX + (upperBoundX - midpointX);
            }

            @SuppressWarnings("SuspiciousNameCombination") Double newVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer y, Integer z) {
                    return f.evaluate(newX, y, z);
                }
            }, lowerBoundY, upperBoundY, lowerBoundZ, upperBoundZ).getThird();

            if (newVal > xMidpointVal) {
                if (midpointX - lowerBoundX > upperBoundX - midpointX) {
                    return fibonacciMax3VarImpl(f, lowerBoundX, newX, midpointX, newVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
                else {
                    return fibonacciMax3VarImpl(f, midpointX, newX, upperBoundX, newVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
            }
            else {
                if (midpointX - lowerBoundX > upperBoundX - midpointX) {
                    return fibonacciMax3VarImpl(f, newX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
                else {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, newX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
            }
        }
        else if (deltaY > deltaZ) { // deltaY is the biggest - we want to shrink deltaY
            final Integer newY;
            if (midpointY - lowerBoundY > upperBoundY - midpointY) {
                newY = upperBoundY - (midpointY - lowerBoundY);
            }
            else {
                newY = lowerBoundY + (upperBoundY - midpointY);
            }

            Double newVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer z) {
                    return f.evaluate(x, newY, z);
                }
            }, lowerBoundX, upperBoundX, lowerBoundZ, upperBoundZ).getThird();

            if (newVal > yMidpointVal) {
                if (midpointY - lowerBoundY > upperBoundY - midpointY) {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, newY, midpointY, newVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
                else {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   midpointY, newY, upperBoundY, newVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
            }
            else {
                if (midpointY - lowerBoundY > upperBoundY - midpointY) {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   newY, midpointY, upperBoundY, yMidpointVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
                else {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, newY, yMidpointVal,
                                                   lowerBoundZ, midpointZ, upperBoundZ, zMidpointVal);
                }
            }
        }
        else { // deltaZ is the biggest - we want to shrink deltaZ
            final Integer newZ;
            if (midpointZ - lowerBoundZ > upperBoundZ - midpointZ) {
                newZ = upperBoundZ - (midpointZ - lowerBoundZ);
            }
            else {
                newZ = lowerBoundZ + (upperBoundZ - midpointZ);
            }

            Double newVal = fibonacciMax2Var(new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer y) {
                    return f.evaluate(x, y, newZ);
                }
            }, lowerBoundX, upperBoundX, lowerBoundY, upperBoundY).getThird();

            if (newVal > zMidpointVal) {
                if (midpointZ - lowerBoundZ > upperBoundZ - midpointZ) {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   lowerBoundZ, newZ, midpointZ, newVal);
                }
                else {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   midpointZ, newZ, upperBoundZ, newVal);
                }
            }
            else {
                if (midpointZ - lowerBoundZ > upperBoundZ - midpointZ) {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   newZ, midpointZ, upperBoundZ, zMidpointVal);
                }
                else {
                    return fibonacciMax3VarImpl(f, lowerBoundX, midpointX, upperBoundX, xMidpointVal,
                                                   lowerBoundY, midpointY, upperBoundY, yMidpointVal,
                                                   lowerBoundZ, midpointZ, newZ, zMidpointVal);
                }
            }
        }
    }
}
