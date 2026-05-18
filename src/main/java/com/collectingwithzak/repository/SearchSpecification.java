package com.collectingwithzak.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class SearchSpecification {

    private SearchSpecification() {}

    public static <T> Specification<T> multiTermLike(String query, List<String> fields) {
        String[] terms = query.trim().toLowerCase().split("\\s+");

        return (root, criteriaQuery, criteriaBuilder) -> {
            Predicate[] termPredicates = new Predicate[terms.length];

            for (int i = 0; i < terms.length; i++) {
                String pattern = "%" + terms[i] + "%";
                Predicate[] fieldPredicates = fields.stream()
                        .map(field -> criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), pattern))
                        .toArray(Predicate[]::new);
                termPredicates[i] = criteriaBuilder.or(fieldPredicates);
            }

            criteriaQuery.orderBy(criteriaBuilder.asc(root.get(fields.get(0))));
            return criteriaBuilder.and(termPredicates);
        };
    }
}
