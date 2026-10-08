package com.campusone.common;

import com.campusone.common.util.BusinessNumberGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessNumberGeneratorTest {

    @Test
    void generatesReadableUniqueNumbers() {
        Set<String> numbers = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String number = BusinessNumberGenerator.generate("APP");
            assertTrue(number.matches("APP\\d{17}[A-F0-9]{6}"));
            numbers.add(number);
        }
        assertEquals(500, numbers.size());
    }

    @Test
    void rejectsBlankPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> BusinessNumberGenerator.generate(" "));
    }
}
