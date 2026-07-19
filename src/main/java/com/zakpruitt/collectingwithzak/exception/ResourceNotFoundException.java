package com.zakpruitt.collectingwithzak.exception;

import java.util.function.Supplier;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s not found with id: %d", resource, id));
    }

    /**
     * Supplier form for {@code repository.findById(id).orElseThrow(notFound("Sale", id))}.
     */
    public static Supplier<ResourceNotFoundException> notFound(String resource, Long id) {
        return () -> new ResourceNotFoundException(resource, id);
    }
}
