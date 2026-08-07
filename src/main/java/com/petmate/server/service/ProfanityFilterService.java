package com.petmate.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProfanityFilterService {

    private final Set<String> badWords = new HashSet<>();
    private Pattern badWordsPattern;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("bad_words.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line1 = reader.readLine(); // comment
                    String line2 = reader.readLine(); // words with comma
                    
                    if (line2 != null && line2.startsWith("[") && line2.endsWith("]")) {
                        // Remove brackets
                        line2 = line2.substring(1, line2.length() - 1);
                        // Split by comma
                        String[] words = line2.split(",");
                        for (String word : words) {
                            // Remove single quotes
                            String cleanWord = word.trim().replaceAll("^'|'$", "");
                            if (!cleanWord.isEmpty()) {
                                badWords.add(cleanWord.toLowerCase());
                            }
                        }
                    }
                }
                
                // Build regex pattern for fast replacement
                if (!badWords.isEmpty()) {
                    StringBuilder patternBuilder = new StringBuilder();
                    for (String word : badWords) {
                        if (patternBuilder.length() > 0) {
                            patternBuilder.append("|");
                        }
                        patternBuilder.append(Pattern.quote(word));
                    }
                    // Use case-insensitive matching, surrounded by non-letter characters or string boundaries
                    badWordsPattern = Pattern.compile("(?iu)(?<=^|\\P{L})(" + patternBuilder.toString() + ")(?=\\P{L}|$)");
                    log.info("Loaded {} bad words into ProfanityFilterService.", badWords.size());
                } else {
                    log.warn("Profanity filter is empty. Check bad_words.txt format.");
                }
            } else {
                log.warn("bad_words.txt not found in classpath.");
            }
        } catch (Exception e) {
            log.error("Failed to load bad_words.txt", e);
        }
    }

    public String filter(String input) {
        if (input == null || input.isEmpty() || badWordsPattern == null) {
            return input;
        }
        
        Matcher matcher = badWordsPattern.matcher(input);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String match = matcher.group();
            // Replace with asterisks of same length
            matcher.appendReplacement(result, "*".repeat(match.length()));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
