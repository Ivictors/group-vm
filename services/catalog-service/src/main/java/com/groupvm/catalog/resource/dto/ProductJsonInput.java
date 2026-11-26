package com.groupvm.catalog.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Set;

final class ProductJsonInput {
    private ProductJsonInput() {}

    static void object(JsonNode body, String... allowed) {
        if (body == null || !body.isObject()) throw invalid("JSON object is required");
        Set<String> names = Set.of(allowed);
        body.fieldNames().forEachRemaining(name -> {
            if (!names.contains(name)) throw invalid("Unexpected field: " + name);
        });
    }

    static String text(JsonNode body, String field, boolean required) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            if (required || (node != null && !field.equals("description"))) throw invalid(field + " is required");
            return null;
        }
        if (!node.isTextual()) throw invalid(field + " must be a string");
        String value = node.textValue();
        if (value.indexOf('\0') >= 0) throw invalid(field + " contains a forbidden character");
        int maxLength = field.equals("sku") ? 80 : field.equals("name") ? 160 : 2000;
        String lengthCheckedValue = field.equals("description") ? value : value.strip();
        if (lengthCheckedValue.length() > maxLength || (!field.equals("description") && lengthCheckedValue.isEmpty())) {
            throw invalid(field + " is invalid");
        }
        return value;
    }

    static BigDecimal price(JsonNode body, boolean required) {
        JsonNode node = body.get("price");
        if (node == null) {
            if (required) throw invalid("price is required");
            return null;
        }
        object(node, "currency", "amount");
        JsonNode currency = node.get("currency");
        JsonNode amount = node.get("amount");
        if (currency == null || !currency.isTextual() || !"BRL".equals(currency.textValue())
                || amount == null || !amount.isTextual()
                || amount.textValue().length() > 20
                || !amount.textValue().matches("^(0|[1-9][0-9]*)\\.[0-9]{2}$")) {
            throw invalid("price must be positive BRL with exactly two decimal places");
        }
        BigDecimal value = new BigDecimal(amount.textValue());
        if (value.signum() <= 0 || value.precision() - value.scale() > 17) {
            throw invalid("price must be positive BRL with exactly two decimal places");
        }
        return value;
    }

    static Integer weight(JsonNode body, boolean required) {
        JsonNode node = body.get("weightGrams");
        if (node == null) {
            if (required) throw invalid("weightGrams is required");
            return null;
        }
        if (!node.isIntegralNumber() || !node.canConvertToInt() || node.intValue() <= 0) {
            throw invalid("weightGrams must be a positive integer");
        }
        return node.intValue();
    }

    static BigDecimal dimension(JsonNode body, String field, boolean required) {
        JsonNode node = body.get(field);
        if (node == null) {
            if (required) throw invalid(field + " is required");
            return null;
        }
        if (!node.isNumber() || node.decimalValue().signum() <= 0) {
            throw invalid(field + " must be a positive number");
        }
        return node.decimalValue();
    }

    private static InvalidProductRequestException invalid(String message) {
        return new InvalidProductRequestException(message);
    }
}
