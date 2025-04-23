package ru.sibsutis.artificial_basis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SimplexProblem {
    private Goal goal;
    private List<Fraction> zCoefficients;
    private List<Equation> equations;
}
