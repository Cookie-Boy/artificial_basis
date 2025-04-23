package ru.sibsutis.artificial_basis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Equation {
    private List<Fraction> coefficients;
    private String sign;
    private Fraction result;
}
