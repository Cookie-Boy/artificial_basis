package ru.sibsutis.artificial_basis;

import lombok.Getter;

import java.math.BigInteger;
import java.util.Objects;

@Getter
public class Fraction implements Comparable<Fraction> {
    private BigInteger numerator;
    private BigInteger denominator;

    public static final Fraction ZERO = new Fraction(0);
    public static final Fraction ONE = new Fraction(1);
    public static final Fraction MINUS_ONE = new Fraction(-1);
    public static final Fraction MIN_VALUE = new Fraction(Integer.MIN_VALUE);
    public static final Fraction MAX_VALUE = new Fraction(Integer.MAX_VALUE);

    public Fraction(BigInteger numerator, BigInteger denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
        simplify();
    }

    public Fraction(int numerator, int denominator) {
        this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public Fraction(int numerator) {
        this(BigInteger.valueOf(numerator), BigInteger.ONE);
    }

    private void simplify() {
        if (denominator.equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("Denominator cannot be zero.");
        }
        BigInteger gcdValue = numerator.gcd(denominator);
        numerator = numerator.divide(gcdValue);
        denominator = denominator.divide(gcdValue);
        if (denominator.compareTo(BigInteger.ZERO) < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
    }

    public static Fraction parseFraction(String string) {
        if (string.contains("/")) {
            String[] parts = string.split("/");
            return new Fraction(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return new Fraction(Integer.parseInt(string));
    }

    public Fraction add(Fraction other) {
        BigInteger newNumerator = this.numerator.multiply(other.denominator)
                .add(other.numerator.multiply(this.denominator));
        BigInteger newDenominator = this.denominator.multiply(other.denominator);
        return new Fraction(newNumerator.intValue(), newDenominator.intValue());
    }

    public Fraction subtract(Fraction other) {
        BigInteger newNumerator = this.numerator.multiply(other.denominator)
                .subtract(other.numerator.multiply(this.denominator));
        BigInteger newDenominator = this.denominator.multiply(other.denominator);
        return new Fraction(newNumerator.intValue(), newDenominator.intValue());
    }

    public Fraction multiply(Fraction other) {
        BigInteger newNumerator = this.numerator.multiply(other.numerator);
        BigInteger newDenominator = this.denominator.multiply(other.denominator);
        return new Fraction(newNumerator.intValue(), newDenominator.intValue());
    }

    public Fraction multiply(int other) {
        return new Fraction(this.numerator.intValue() * other, this.denominator.intValue());
    }

    public Fraction divide(Fraction other) {
        BigInteger newNumerator = this.numerator.multiply(other.denominator);
        BigInteger newDenominator = this.denominator.multiply(other.numerator);
        return new Fraction(newNumerator.intValue(), newDenominator.intValue());
    }

    public Fraction abs() {
        int newNumerator = Math.abs(this.numerator.intValue());
        int newDenominator = Math.abs(this.denominator.intValue());
        return new Fraction(newNumerator, newDenominator);
    }

    @Override
    public String toString() {
        if (denominator.equals(BigInteger.ONE)) {
            return numerator.toString();
        }
        return numerator + "/" + denominator;
    }

    @Override
    public int compareTo(Fraction other) {
        return this.numerator.multiply(other.denominator)
                .compareTo(other.numerator.multiply(this.denominator));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fraction fraction = (Fraction) o;

        BigInteger thisNumerator = this.numerator.multiply(fraction.denominator);
        BigInteger otherNumerator = fraction.numerator.multiply(this.denominator);

        return thisNumerator.equals(otherNumerator);
    }

    @Override
    public int hashCode() {
        Fraction simplified = new Fraction(numerator.intValue(), denominator.intValue());
        return Objects.hash(simplified.numerator, simplified.denominator);
    }
}
